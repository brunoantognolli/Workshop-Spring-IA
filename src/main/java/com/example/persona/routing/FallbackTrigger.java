package com.example.persona.routing;

/**
 * Defines when the engine escalates from the primary (local) model
 * to the configured cloud fallback model.
 */
public enum FallbackTrigger {

    /**
     * Escalate only when the primary model's output fails the
     * output contract validation (schema or guardrail violation).
     * This is the recommended default: runs locally as much as possible.
     */
    CONTRACT_FAILURE,

    /**
     * Escalate whenever the primary model throws an exception
     * (e.g., timeout, Ollama unreachable, parsing error).
     */
    ON_ERROR,

    /**
     * Always use the cloud fallback model, bypassing the local model entirely.
     * Useful during development when Ollama is not running.
     */
    ALWAYS
}
