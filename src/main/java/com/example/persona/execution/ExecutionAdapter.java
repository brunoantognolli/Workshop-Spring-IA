package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;

/**
 * Adapts a persona's prompt and response for a specific execution environment.
 *
 * <p>The adapter layer separates the persona's semantic behavior (what to do)
 * from the output formatting (how to present the result). A persona defines
 * the role, boundaries, and skills — the adapter decides whether the response
 * should be natural language, JSON, raw code, or a unified diff.
 *
 * <h3>Lifecycle:</h3>
 * <ol>
 *   <li>{@link #supports(ExecutionContext)} — checks if this adapter handles the mode</li>
 *   <li>{@link #adaptPrompt} — appends execution-specific instructions to the system prompt</li>
 *   <li>(LLM call happens in PersonaEngine)</li>
 *   <li>{@link #adaptResponse} — post-processes the raw LLM output</li>
 * </ol>
 *
 * @see ChatExecutionAdapter
 * @see IdeEditExecutionAdapter
 * @see IdeApplyExecutionAdapter
 * @see JsonApiExecutionAdapter
 */
public interface ExecutionAdapter {

    /**
     * Returns {@code true} if this adapter handles the given execution context.
     *
     * @param context the resolved execution context
     * @return true if this adapter should be used
     */
    boolean supports(ExecutionContext context);

    /**
     * Adapts the system prompt by appending execution-specific instructions.
     *
     * <p>The original system prompt (built from the persona's role, boundaries,
     * and format instructions) is passed in and returned with additional
     * instructions appended. The persona's core prompt is never replaced.
     *
     * @param persona      the persona definition
     * @param context      the execution context
     * @param systemPrompt the base system prompt built by PersonaEngine
     * @return the adapted system prompt
     */
    String adaptPrompt(PersonaDefinition persona, ExecutionContext context, String systemPrompt);

    /**
     * Post-processes the raw LLM response for the target execution environment.
     *
     * <p>For example, the {@link IdeEditExecutionAdapter} strips markdown fences
     * and removes explanatory text, leaving only the code.
     *
     * @param persona     the persona definition
     * @param context     the execution context
     * @param rawResponse the raw text from the LLM
     * @return the adapted response
     */
    String adaptResponse(PersonaDefinition persona, ExecutionContext context, String rawResponse);
}
