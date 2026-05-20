package com.example.persona.execution;

/**
 * Defines the expected output format for a persona response.
 *
 * <p>Each {@link ExecutionMode} maps to a default {@code OutputStrategy},
 * but personas can override this via their YAML {@code execution:} configuration.
 */
public enum OutputStrategy {

    /** Free-form text or markdown — the persona's natural response. */
    NATURAL_LANGUAGE,

    /** JSON conforming to the persona's declared output_contract schema. */
    STRUCTURED_JSON,

    /** Complete updated file content — no diff, no commentary. */
    FULL_FILE,

    /** Unified diff format (e.g., {@code --- a/file +++ b/file}). */
    UNIFIED_DIFF,

    /** Raw code block only — no markdown fences, no explanations. */
    CODE_BLOCK_ONLY,

    /** Patch-only output — strictly applicable, no commentary at all. */
    PATCH_ONLY
}
