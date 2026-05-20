package com.example.persona.api.service;

import com.example.persona.api.dto.ChatCompletionResponse;
import com.example.persona.api.dto.ModelListResponse;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.api.support.StreamingErrorFormatter;
import com.example.persona.execution.ExecutionMode;
import com.example.persona.execution.ModelNameParser;
import com.example.persona.execution.PersonaRuntimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.persona.core.PersonaEngine.PersonaExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service layer for the OpenAI-compatible API.
 *
 * <p>Delegates to {@link PersonaRuntimeService} for adapter-aware execution
 * (edit/apply modes) and falls back to direct {@link PersonaEngine} calls
 * for standard chat mode to preserve backward compatibility.
 */
@Service
public class OpenAiCompatibleService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleService.class);

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;
    private final PersonaRuntimeService runtimeService;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleService(PersonaLoader personaLoader,
                                    PersonaEngine personaEngine,
                                    PersonaRuntimeService runtimeService) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
        this.runtimeService = runtimeService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lists all available personas as models, including execution mode variants
     * (e.g., {@code javadoc-persona}, {@code javadoc-persona:edit}, {@code javadoc-persona:apply}).
     */
    public ModelListResponse listModels() {
        List<String> baseIds = personaLoader.loadAll().stream()
                .map(p -> p.id())
                .toList();

        List<String> allModelIds = new ArrayList<>();
        for (String id : baseIds) {
            allModelIds.add(id);             // chat mode (default)
            allModelIds.add(id + ":edit");   // IDE edit mode
            allModelIds.add(id + ":apply");  // IDE apply mode
        }

        return ModelListResponse.of(allModelIds);
    }

    /**
     * Processes a chat completion request, routing to the appropriate
     * execution path based on the model name.
     *
     * <p>Model names containing execution suffixes (e.g., {@code :edit}, {@code :apply})
     * are routed through {@link PersonaRuntimeService} for adapter-aware execution.
     * Plain model names use the existing direct {@link PersonaEngine} path.
     */
    public Object processChatCompletion(String modelName, String userInput, Boolean stream) throws Exception {
        ModelNameParser.ParsedModelName parsed = ModelNameParser.parse(modelName);
        ExecutionMode mode = parsed.executionMode();
        String personaId = parsed.personaId();

        boolean effectiveStream = shouldUseStreaming(stream, mode);
        log.info("Processing chat completion: model='{}', persona='{}', mode={}, stream={}, effectiveStream={}",
                modelName, personaId, mode, stream, effectiveStream);

        // For non-CHAT modes, use the runtime service with adapter support
        if (mode != ExecutionMode.CHAT) {
            return processWithAdapter(modelName, personaId, userInput, stream, mode);
        }

        // For CHAT mode, preserve existing behavior for backward compatibility
        return processDirectChat(personaId, userInput, effectiveStream);
    }

    /**
     * IDE edit/apply must stream for Continue inline diffs. When {@code stream} is null
     * (common for Continue Edit), default to streaming for those modes.
     */
    static boolean shouldUseStreaming(Boolean stream, ExecutionMode mode) {
        if (Boolean.TRUE.equals(stream)) {
            return true;
        }
        if (Boolean.FALSE.equals(stream)) {
            return false;
        }
        return mode == ExecutionMode.IDE_EDIT || mode == ExecutionMode.IDE_APPLY;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Processes a request through the adapter-aware pipeline.
     */
    private Object processWithAdapter(String modelName, String personaId,
                                       String userInput, Boolean stream, ExecutionMode mode) {
        // Continue Edit often omits stream:true; IDE modes must stream or the client times out.
        if (shouldUseStreaming(stream, mode)) {
            return createStreamingResponse(modelName, personaId, userInput, true);
        }

        try {
            String content = runtimeService.execute(modelName, userInput);
            return ResponseEntity.ok(ChatCompletionResponse.of(modelName, content));
        } catch (Exception e) {
            throw toPersonaExecutionException(e);
        }
    }

    /**
     * Processes a standard CHAT request using the direct PersonaEngine path.
     * This preserves backward compatibility with existing clients.
     */
    private Object processDirectChat(String personaId, String userInput, boolean useStream) throws Exception {
        var persona = personaLoader.load(personaId);

        if (useStream) {
            return createStreamingResponse(personaId, personaId, userInput, false);
        }

        try {
            Object output = personaEngine.execute(persona, userInput, true);

            // If ignoreContract is true, output is raw string (Markdown/text).
            // If it were false, it would be the validated Record, requiring serialization.
            String content = output instanceof String ? (String) output :
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);

            return ResponseEntity.ok(ChatCompletionResponse.of(personaId, content));
        } catch (Exception e) {
            throw toPersonaExecutionException(e);
        }
    }

    /**
     * Creates a streaming SSE response. When {@code useAdapter} is true,
     * streams through the runtime service (with adapter prompt adaptation).
     */
    private SseEmitter createStreamingResponse(String modelName, String personaId,
                                                String userInput, boolean useAdapter) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        Flux<String> tokenStream;
        String sseModel = modelName != null ? modelName : personaId;

        if (useAdapter && modelName != null) {
            tokenStream = runtimeService.stream(modelName, userInput);
        } else {
            var persona = personaLoader.load(personaId);
            tokenStream = personaEngine.streamUnvalidated(persona, userInput, true);
        }

        AtomicBoolean isFirst = new AtomicBoolean(true);

        tokenStream.subscribe(
                token -> {
                    try {
                        emitter.send(SseEmitter.event().data(createSseChunk(sseModel, token, isFirst.getAndSet(false))));
                    } catch (Exception e) {
                        completeStreamWithError(emitter, sseModel, e);
                    }
                },
                error -> completeStreamWithError(emitter, sseModel, error),
                () -> completeStreamSuccessfully(emitter, sseModel)
        );

        emitter.onTimeout(() -> completeStreamWithError(emitter, sseModel,
                new PersonaExecutionException("Streaming timed out after 3 minutes.")));

        return emitter;
    }

    /**
     * Sends a readable error as SSE content, then closes the stream without triggering
     * Spring's JSON error handler on an already-committed {@code text/event-stream} response.
     */
    private void completeStreamWithError(SseEmitter emitter, String personaId, Throwable error) {
        String message = StreamingErrorFormatter.toReadableMessage(error);
        log.error("Streaming failed for model '{}': {}", personaId, message, error);
        try {
            emitter.send(SseEmitter.event().data(createSseChunk(personaId, message, true)));
            emitter.send(SseEmitter.event().data(createSseFinalChunk(personaId, "stop")));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception sendError) {
            log.warn("Could not deliver SSE error payload, closing stream: {}", sendError.getMessage());
            emitter.complete();
        }
    }

    private void completeStreamSuccessfully(SseEmitter emitter, String model) {
        try {
            emitter.send(SseEmitter.event().data(createSseFinalChunk(model, "stop")));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            log.warn("Could not close SSE stream cleanly: {}", e.getMessage());
            emitter.complete();
        }
    }

    private PersonaExecutionException toPersonaExecutionException(Exception e) {
        if (e instanceof PersonaExecutionException personaEx) {
            return personaEx;
        }
        return new PersonaExecutionException(StreamingErrorFormatter.toReadableMessage(e), e);
    }

    private String createSseChunk(String personaId, String token, boolean isFirst) {
        try {
            Map<String, Object> delta = new java.util.HashMap<>();
            if (isFirst) {
                delta.put("role", "assistant");
            }
            delta.put("content", token);

            Map<String, Object> choice = new java.util.HashMap<>();
            choice.put("index", 0);
            choice.put("delta", delta);
            choice.put("finish_reason", null);

            Map<String, Object> chunk = Map.of(
                    "id", "chatcmpl-" + System.currentTimeMillis(),
                    "object", "chat.completion.chunk",
                    "created", System.currentTimeMillis() / 1000,
                    "model", personaId,
                    "choices", List.of(choice)
            );
            return objectMapper.writeValueAsString(chunk);
        } catch (Exception e) {
            log.error("Failed to serialize SSE chunk", e);
            return "";
        }
    }

    private String createSseFinalChunk(String personaId, String finishReason) {
        try {
            Map<String, Object> choice = Map.of(
                    "index", 0,
                    "delta", Map.of(),
                    "finish_reason", finishReason
            );
            Map<String, Object> chunk = Map.of(
                    "id", "chatcmpl-" + System.currentTimeMillis(),
                    "object", "chat.completion.chunk",
                    "created", System.currentTimeMillis() / 1000,
                    "model", personaId,
                    "choices", List.of(choice)
            );
            return objectMapper.writeValueAsString(chunk);
        } catch (Exception e) {
            log.error("Failed to serialize SSE final chunk", e);
            return "";
        }
    }
}
