# Persona as Code — Run Example (Windows PowerShell)
# Usage: .\run-example.ps1 [-Persona <id>] [-InputFile <path>] [-FallbackKey <key>]

param(
    [string]$Persona   = "javadoc-persona",
    [string]$InputFile = "",
    [string]$FallbackKey = ""
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Persona as Code - PoC Runner" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. Check Ollama ─────────────────────────────────────────────────────────
Write-Host "[1/4] Checking Ollama..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method Get -TimeoutSec 5
    Write-Host "      Ollama is running." -ForegroundColor Green
} catch {
    Write-Host "      Ollama not detected. Starting via Docker..." -ForegroundColor Yellow
    docker-compose up -d
    Start-Sleep -Seconds 5

    Write-Host "      Pulling mistral model (first run may take a few minutes)..." -ForegroundColor Yellow
    docker-compose exec -T ollama ollama pull mistral
}

# ── 2. Build the JAR ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[2/4] Building application..." -ForegroundColor Yellow
mvn package -q -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed!"; exit 1 }
Write-Host "      Build successful." -ForegroundColor Green

# ── 3. Set environment and start server ─────────────────────────────────────
Write-Host ""
Write-Host "[3/4] Starting Persona Engine on port 8080..." -ForegroundColor Yellow

$env:OLLAMA_BASE_URL = "http://localhost:11434"
if ($FallbackKey) { $env:OPENAI_API_KEY = $FallbackKey }

$serverProcess = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "target\persona-engine-1.0.0.jar" `
    -PassThru -NoNewWindow
Start-Sleep -Seconds 8

# ── 4. Run example request ───────────────────────────────────────────────────
Write-Host ""
Write-Host "[4/4] Running persona: $Persona" -ForegroundColor Yellow

if ($InputFile -and (Test-Path $InputFile)) {
    $inputContent = Get-Content -Raw $InputFile
} else {
    # Default sample Java class
    $inputContent = @"
public class ShoppingCart {
    private final List<CartItem> items = new ArrayList<>();
    private final DiscountService discountService;

    public ShoppingCart(DiscountService discountService) {
        this.discountService = discountService;
    }

    public void addItem(CartItem item) {
        items.add(item);
    }

    public double calculateTotal() {
        double subtotal = items.stream()
            .mapToDouble(i -> i.getPrice() * i.getQuantity())
            .sum();
        return discountService.apply(subtotal);
    }

    public void clear() {
        items.clear();
    }
}
"@
}

$body = @{ input = $inputContent } | ConvertTo-Json -Depth 5

try {
    $response = Invoke-RestMethod `
        -Uri "http://localhost:8080/api/personas/$Persona/run" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body

    Write-Host ""
    Write-Host "=====================================================" -ForegroundColor Green
    Write-Host "  Result from persona: $Persona" -ForegroundColor Green
    Write-Host "=====================================================" -ForegroundColor Green
    $response.output | ConvertTo-Json -Depth 10 | Write-Host

    # Save output
    $outputPath = "target\persona-output-$Persona.json"
    $response.output | ConvertTo-Json -Depth 10 | Set-Content $outputPath
    Write-Host ""
    Write-Host "Output saved to: $outputPath" -ForegroundColor Cyan

} catch {
    Write-Host "Request failed: $_" -ForegroundColor Red
} finally {
    # Stop the server
    Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    Write-Host "Server stopped." -ForegroundColor Gray
}

Write-Host ""
Write-Host "Done! To test the OpenAI-compatible API manually:" -ForegroundColor Cyan
Write-Host '  curl http://localhost:8080/v1/models' -ForegroundColor White
Write-Host '  curl -X POST http://localhost:8080/v1/chat/completions -H "Content-Type: application/json" -d "{\"model\":\"javadoc-persona\",\"messages\":[{\"role\":\"user\",\"content\":\"Document: public class Foo {}\"}]}"' -ForegroundColor White
