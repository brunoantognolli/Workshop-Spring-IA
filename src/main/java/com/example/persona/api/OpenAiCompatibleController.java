package com.example.persona.api;

import com.example.persona.api.dto.ChatCompletionRequest;
import com.example.persona.api.dto.ChatCompletionResponse;
import com.example.persona.api.dto.ModelListResponse;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible API that exposes personas as "models".
 *
 * <p>Any tool that supports a custom OpenAI base URL can use this API to interact
 * with personas as if they were AI models:
 * <ul>
 *   <li><b>Cursor IDE</b>: Settings → Models → Add custom model (base URL = http://localhost:8080/v1)</li>
 *   <li><b>Continue.dev</b>: config.json → models → openai provider with custom baseURL</li>
 *   <li><b>Open WebUI</b>: Settings → Connections → OpenAI API → base URL</li>
 *   <li><b>LM Studio</b>: API server tab → OpenAI compatibility</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code GET  /v1/models}               — lists all personas as models</li>
 *   <li>{@code POST /v1/chat/completions}      — runs the persona specified in {@code model} field</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1")
public class OpenAiCompatibleController {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleController.class);

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleController(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lists all available personas as OpenAI-format model objects.
     * Used by clients to populate their model selector.
     */
    @GetMapping("/models")
    public ModelListResponse listModels() {
        List<String> personaIds = personaLoader.loadAll().stream()
                .map(p -> p.id())
                .toList();
        return ModelListResponse.of(personaIds);
    }

    /**
     * Executes a chat completion using the persona specified in the {@code model} field.
     *
     * <p>The last {@code user} message in the {@code messages} array is used as the
     * persona input. System messages are ignored (each persona has its own system prompt).
     *
     * <p>Example request:
     * <pre>{@code
     * POST /v1/chat/completions
     * {
     *   "model": "javadoc-persona",
     *   "messages": [
     *     { "role": "user", "content": "Document this: public class Foo {...}" }
     *   ]
     * }
     * }</pre>
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(@RequestBody ChatCompletionRequest request) {
        String personaId = request.model();
        log.info("OpenAI-compat request: model={}", personaId);

        // Extract the last user message as the persona input
        String userInput = request.messages().stream()
                .filter(m -> "user".equalsIgnoreCase(m.role()))
                .reduce((first, second) -> second)
                .map(ChatCompletionRequest.Message::content)
                .orElse("");

        if (userInput.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user message found in request"));
        }

        try {
            var persona = personaLoader.load(personaId);
            Object output = personaEngine.execute(persona, userInput);

            // Serialize output to JSON string (the "content" in the OpenAI response)
            String content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(output);

            return ResponseEntity.ok(ChatCompletionResponse.of(personaId, content));

        } catch (PersonaLoader.PersonaLoadException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "model_not_found",
                                 "message", "Persona '" + personaId + "' not found. " +
                                            "Use GET /v1/models to list available personas."));
        } catch (Exception e) {
            log.error("Chat completion failed for persona '{}': {}", personaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal_error", "message", e.getMessage()));
        }
    }
}
