package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.springframework.stereotype.Component;

/**
 * Adapter for {@link ExecutionMode#CHAT} and {@link ExecutionMode#ANALYSIS} modes.
 *
 * <p>Passes the persona's prompt and response through unchanged. This is the
 * default behavior — the persona responds naturally with text, markdown, or
 * whatever format it was configured to produce.
 */
@Component
public class ChatExecutionAdapter implements ExecutionAdapter {

    @Override
    public boolean supports(ExecutionContext context) {
        return context.executionMode() == ExecutionMode.CHAT
                || context.executionMode() == ExecutionMode.ANALYSIS
                || context.executionMode() == ExecutionMode.CI_REVIEW;
    }

    /**
     * Returns the system prompt unchanged — no additional instructions needed.
     */
    @Override
    public String adaptPrompt(PersonaDefinition persona, ExecutionContext context, String systemPrompt) {
        return systemPrompt;
    }

    /**
     * Returns the raw response unchanged — the persona's natural output.
     */
    @Override
    public String adaptResponse(PersonaDefinition persona, ExecutionContext context, String rawResponse) {
        return rawResponse;
    }
}
