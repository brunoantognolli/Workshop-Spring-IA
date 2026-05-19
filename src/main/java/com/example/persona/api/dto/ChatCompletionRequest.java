package com.example.persona.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI-compatible Chat Completion request.
 * Mirrors the format expected by {@code POST /v1/chat/completions}.
 *
 * <p>The {@code model} field is used to select which persona to execute.
 * Example: {@code "model": "javadoc-persona"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        Boolean stream
) {
    /** A single chat message (role + content). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}
}
