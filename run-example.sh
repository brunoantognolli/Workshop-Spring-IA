#!/usr/bin/env bash
# Persona as Code — Run Example (Linux/Mac)
# Usage: ./run-example.sh [persona-id] [input-file] [fallback-api-key]

set -e

PERSONA="${1:-javadoc-persona}"
INPUT_FILE="${2:-}"
FALLBACK_KEY="${3:-}"

echo ""
echo "====================================================="
echo "  Persona as Code - PoC Runner"
echo "====================================================="
echo ""

# ── 1. Check Ollama ──────────────────────────────────────────────────────────
echo "[1/4] Checking Ollama..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "      Ollama is running."
else
    echo "      Starting Ollama via Docker..."
    docker-compose up -d
    sleep 5
    echo "      Pulling mistral model..."
    docker-compose exec -T ollama ollama pull mistral
fi

# ── 2. Build ─────────────────────────────────────────────────────────────────
echo ""
echo "[2/4] Building application..."
mvn package -q -DskipTests
echo "      Build successful."

# ── 3. Start server ──────────────────────────────────────────────────────────
echo ""
echo "[3/4] Starting Persona Engine on port 8080..."
export OLLAMA_BASE_URL="http://localhost:11434"
[ -n "$FALLBACK_KEY" ] && export OPENAI_API_KEY="$FALLBACK_KEY"

java -jar target/persona-engine-1.0.0.jar &
SERVER_PID=$!
sleep 8

# ── 4. Run example ──────────────────────────────────────────────────────────
echo ""
echo "[4/4] Running persona: $PERSONA"

if [ -n "$INPUT_FILE" ] && [ -f "$INPUT_FILE" ]; then
    INPUT_CONTENT=$(cat "$INPUT_FILE")
else
    INPUT_CONTENT='public class ShoppingCart {
    private final List<CartItem> items = new ArrayList<>();
    private final DiscountService discountService;
    public ShoppingCart(DiscountService discountService) { this.discountService = discountService; }
    public void addItem(CartItem item) { items.add(item); }
    public double calculateTotal() {
        double subtotal = items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        return discountService.apply(subtotal);
    }
    public void clear() { items.clear(); }
}'
fi

BODY=$(printf '{"input": %s}' "$(echo "$INPUT_CONTENT" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')")

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/personas/$PERSONA/run" \
    -H "Content-Type: application/json" \
    -d "$BODY")

echo ""
echo "====================================================="
echo "  Result from persona: $PERSONA"
echo "====================================================="
echo "$RESPONSE" | python3 -m json.tool

OUTPUT_FILE="target/persona-output-${PERSONA}.json"
echo "$RESPONSE" > "$OUTPUT_FILE"
echo ""
echo "Output saved to: $OUTPUT_FILE"

# ── Cleanup ──────────────────────────────────────────────────────────────────
kill $SERVER_PID 2>/dev/null || true
echo "Server stopped."

echo ""
echo "To test the OpenAI-compatible API manually:"
echo "  curl http://localhost:8080/v1/models"
echo '  curl -X POST http://localhost:8080/v1/chat/completions \'
echo '    -H "Content-Type: application/json" \'
echo "    -d '{\"model\":\"$PERSONA\",\"messages\":[{\"role\":\"user\",\"content\":\"Document: public class Foo {}\"}]}'"
