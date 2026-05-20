package com.example.persona.execution;

/**
 * Defines the execution context in which a persona's output will be consumed.
 *
 * <p>The execution mode determines how the persona's response is formatted
 * and post-processed by the {@link ExecutionAdapter} layer. The persona's
 * semantic behavior (role, boundaries, skills) remains unchanged — only
 * the output format is adapted.
 *
 * <h3>Mode-to-OutputStrategy defaults:</h3>
 * <ul>
 *   <li>{@code CHAT}      → {@link OutputStrategy#NATURAL_LANGUAGE}</li>
 *   <li>{@code ANALYSIS}  → {@link OutputStrategy#NATURAL_LANGUAGE}</li>
 *   <li>{@code IDE_EDIT}  → {@link OutputStrategy#FULL_FILE} or {@link OutputStrategy#CODE_BLOCK_ONLY}</li>
 *   <li>{@code IDE_APPLY} → {@link OutputStrategy#PATCH_ONLY} or {@link OutputStrategy#FULL_FILE}</li>
 *   <li>{@code CI_REVIEW} → {@link OutputStrategy#NATURAL_LANGUAGE}</li>
 *   <li>{@code JSON_API}  → {@link OutputStrategy#STRUCTURED_JSON}</li>
 * </ul>
 */
public enum ExecutionMode {

    /** Natural conversation — persona responds freely with text/markdown. */
    CHAT,

    /** Structured analysis — similar to chat but may include formatted sections. */
    ANALYSIS,

    /** IDE edit action — response must be applicable code, no explanations. */
    IDE_EDIT,

    /** IDE apply action — strictest mode, produces only code or unified diff. */
    IDE_APPLY,

    /** CI pipeline review — structured review output for automated pipelines. */
    CI_REVIEW,

    /** JSON API — respects the persona's declared output_contract schema. */
    JSON_API
}
