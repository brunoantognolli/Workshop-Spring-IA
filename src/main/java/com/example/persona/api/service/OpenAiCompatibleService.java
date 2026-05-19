package com.example.persona.api.service;

import com.example.persona.api.dto.ChatCompletionResponse;
import com.example.persona.api.dto.ModelListResponse;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OpenAiCompatibleService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleService.class);

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleService(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
        this.objectMapper = new ObjectMapper();
    }

    public ModelListResponse listModels() {
        List<String> personaIds = personaLoader.loadAll().stream()
                .map(p -> p.id())
                .toList();
        return ModelListResponse.of(personaIds);
    }

    public Object processChatCompletion(String personaId, String userInput, Boolean stream) throws Exception {
        var persona = personaLoader.load(personaId);

        if (Boolean.TRUE.equals(stream)) {
            SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

            Flux<String> markdownStream = Flux.concat(
                    Flux.just(""), // Initial empty chunk to set the role
                    personaEngine.streamUnvalidated(persona, userInput, true)
            );

            AtomicBoolean isFirst = new AtomicBoolean(true);

            markdownStream.subscribe(
                            token -> {
                                try {
                                    emitter.send(createSseChunk(personaId, token, isFirst.getAndSet(false)));
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            () -> {
                                try {
                                    emitter.send("[DONE]");
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            }
                    );
            return emitter;
        }

        Object output = personaEngine.execute(persona, userInput, true);

        // Se ignoreContract for true, o output será a string bruta (Markdown/texto).
        // Se fosse false, seria o Record validado, e teríamos que serializar com o objectMapper.
        // Como agora é String, podemos mandar direto.
        String content = output instanceof String ? (String) output : 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);

        return ResponseEntity.ok(ChatCompletionResponse.of(personaId, content));
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
}
