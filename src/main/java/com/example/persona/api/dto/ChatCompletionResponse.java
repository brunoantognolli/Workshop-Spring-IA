package com.example.persona.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * OpenAI-compatible Chat Completion response.
 * Mirrors the format returned by the real OpenAI API so that
 * any OpenAI-compatible client (Cursor, Continue.dev, Open WebUI) can consume it.
 */
public record ChatCompletionResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
) {

    public record Choice(
            int index,
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {}

    public record Message(String role, String content) {}

    public record Usage(
            @JsonProperty("prompt_tokens")     int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens")      int totalTokens
    ) {}

    /** Builds a standard successful response wrapping the given content. */
    public static ChatCompletionResponse of(String personaId, String content) {
        return new ChatCompletionResponse(
                "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                "chat.completion",
                Instant.now().getEpochSecond(),
                personaId,
                List.of(new Choice(0, new Message("assistant", content), "stop")),
                new Usage(-1, -1, -1)
        );
    }
}
