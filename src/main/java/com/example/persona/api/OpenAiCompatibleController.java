package com.example.persona.api;

import com.example.persona.api.dto.ChatCompletionRequest;
import com.example.persona.api.dto.ModelListResponse;
import com.example.persona.api.service.OpenAiCompatibleService;
import com.example.persona.api.support.OpenAiMessageExtractor;
import com.example.persona.api.support.StreamingErrorFormatter;
import com.example.persona.core.PersonaEngine.PersonaExecutionException;
import com.example.persona.core.PersonaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OpenAI-compatible API that exposes personas as "models".
 *
 * <p>
 * Any tool that supports a custom OpenAI base URL can use this API to interact
 * with personas as if they were AI models:
 * <ul>
 * <li><b>Cursor IDE</b>: Settings → Models → Add custom model (base URL =
 * http://localhost:8080/v1)</li>
 * <li><b>Continue.dev</b>: config.json → models → openai provider with custom
 * baseURL</li>
 * <li><b>Open WebUI</b>: Settings → Connections → OpenAI API → base URL</li>
 * <li><b>LM Studio</b>: API server tab → OpenAI compatibility</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 * <li>{@code GET  /v1/models} — lists all personas as models</li>
 * <li>{@code POST /v1/chat/completions} — runs the persona specified in
 * {@code model} field</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1")
public class OpenAiCompatibleController {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleController.class);

    private final OpenAiCompatibleService service;

    public OpenAiCompatibleController(OpenAiCompatibleService service) {
        this.service = service;
    }

    /**
     * Lists all available personas as OpenAI-format model objects.
     * Used by clients to populate their model selector.
     */
    @GetMapping("/models")
    public ModelListResponse listModels() {
        return service.listModels();
    }

    /**
     * Executes a chat completion using the persona specified in the {@code model}
     * field.
     *
     * <p>
     * The last {@code user} message in the {@code messages} array is used as the
     * persona input. System messages are ignored (each persona has its own system
     * prompt).
     */
    @PostMapping("/chat/completions")
    public Object chatCompletions(@RequestBody ChatCompletionRequest request) {
        String modelName = request.model();
        log.info("OpenAI-compat request: model={}, stream={}", modelName, request.stream());

        try {
            String userInput = OpenAiMessageExtractor.extractUserInput(request);
            if (OpenAiMessageExtractor.looksLikeContinueEditRequest(userInput)) {
                log.info("Continue inline edit/apply prompt detected for model={}", modelName);
            }
            return service.processChatCompletion(modelName, userInput, request.stream());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (PersonaLoader.PersonaLoadException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "model_not_found",
                            "message", "Persona for model '" + modelName + "' not found. " +
                                    "Use GET /v1/models to list available personas."));
        } catch (PersonaExecutionException e) {
            log.error("Chat completion failed for model '{}': {}", modelName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "persona_execution_failed", "message", e.getMessage()));
        } catch (Exception e) {
            String message = StreamingErrorFormatter.toReadableMessage(e);
            log.error("Chat completion failed for model '{}': {}", modelName, message, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal_error", "message", message));
        }
    }

}
