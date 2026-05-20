package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter for {@link ExecutionMode#JSON_API} — ensures the response
 * respects the persona's declared {@code output_contract} JSON schema.
 *
 * <p>This adapter preserves the persona's format instructions (JSON output
 * contract) and validates/extracts JSON from the LLM response. If the LLM
 * wraps JSON in markdown code fences, they are stripped.
 */
@Component
public class JsonApiExecutionAdapter implements ExecutionAdapter {

    private static final Logger log = LoggerFactory.getLogger(JsonApiExecutionAdapter.class);

    private static final String JSON_API_INSTRUCTIONS = """
            
            # Execution Mode: JSON API
            You MUST return your response as valid JSON matching the declared output schema.
            Do not include any text outside the JSON object.
            Do not wrap the JSON in Markdown code fences.
            The response must be parseable by a JSON parser without any preprocessing.
            """;

    @Override
    public boolean supports(ExecutionContext context) {
        return context.executionMode() == ExecutionMode.JSON_API;
    }

    @Override
    public String adaptPrompt(PersonaDefinition persona, ExecutionContext context, String systemPrompt) {
        return systemPrompt + JSON_API_INSTRUCTIONS;
    }

    @Override
    public String adaptResponse(PersonaDefinition persona, ExecutionContext context, String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }

        String result = rawResponse.trim();

        // Extract JSON from markdown code fences if present
        result = extractJson(result);

        log.debug("JSON_API response adapted: {} chars → {} chars", rawResponse.length(), result.length());
        return result;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Extracts JSON from a response that may be wrapped in markdown code fences.
     * Example: {@code ```json { ... } ```} → {@code { ... }}
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
