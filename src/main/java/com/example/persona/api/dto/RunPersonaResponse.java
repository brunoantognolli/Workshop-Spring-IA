package com.example.persona.api.dto;

/**
 * Response body for {@code POST /api/personas/{id}/run}.
 *
 * @param personaId  the persona that was executed
 * @param output     the deserialized output object (typed per persona contract)
 * @param modelUsed  which model produced the response ("ollama/mistral" or "openai/gpt-4o-mini")
 * @param fallbackUsed whether the cloud fallback was triggered
 */
public record RunPersonaResponse(
        String personaId,
        Object output,
        String modelUsed,
        boolean fallbackUsed
) {}
