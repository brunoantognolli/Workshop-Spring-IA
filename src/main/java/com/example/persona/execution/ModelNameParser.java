package com.example.persona.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a model name from the OpenAI-compatible API into a persona ID
 * and an {@link ExecutionMode}.
 *
 * <h3>Supported formats:</h3>
 * <table>
 *   <tr><th>Model name</th><th>Persona ID</th><th>Execution mode</th></tr>
 *   <tr><td>{@code javadoc-persona}</td><td>javadoc-persona</td><td>CHAT</td></tr>
 *   <tr><td>{@code javadoc-persona:edit}</td><td>javadoc-persona</td><td>IDE_EDIT</td></tr>
 *   <tr><td>{@code javadoc-persona:apply}</td><td>javadoc-persona</td><td>IDE_APPLY</td></tr>
 *   <tr><td>{@code unit-test-generator:json}</td><td>unit-test-generator</td><td>JSON_API</td></tr>
 *   <tr><td>{@code adr-reviewer:review}</td><td>adr-reviewer</td><td>CI_REVIEW</td></tr>
 * </table>
 *
 * <p>Unknown suffixes fall back to {@link ExecutionMode#CHAT}.
 */
public final class ModelNameParser {

    private static final Logger log = LoggerFactory.getLogger(ModelNameParser.class);

    private ModelNameParser() {
        // Utility class
    }

    /**
     * Parses the model name into a {@link ParsedModelName} containing the
     * persona ID and resolved execution mode.
     *
     * @param modelName the full model name from the API request (e.g., "javadoc-persona:edit")
     * @return parsed result with persona ID and execution mode
     */
    public static ParsedModelName parse(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name must not be null or blank");
        }

        int colonIndex = modelName.lastIndexOf(':');
        if (colonIndex < 0 || colonIndex == modelName.length() - 1) {
            // No suffix — default to CHAT
            return new ParsedModelName(modelName.trim(), ExecutionMode.CHAT);
        }

        String personaId = modelName.substring(0, colonIndex).trim();
        String suffix = modelName.substring(colonIndex + 1).trim().toLowerCase();

        if (personaId.isEmpty()) {
            throw new IllegalArgumentException("Persona ID must not be empty in model name: " + modelName);
        }

        ExecutionMode mode = resolveSuffix(suffix);
        log.debug("Parsed model name '{}' → persona='{}', mode={}", modelName, personaId, mode);
        return new ParsedModelName(personaId, mode);
    }

    /**
     * Result of parsing a model name.
     *
     * @param personaId     the persona ID without the execution suffix
     * @param executionMode the resolved execution mode
     */
    public record ParsedModelName(String personaId, ExecutionMode executionMode) {}

    // ─── Private helpers ──────────────────────────────────────────────────────

    private static ExecutionMode resolveSuffix(String suffix) {
        return switch (suffix) {
            case "edit", "ide-edit" -> ExecutionMode.IDE_EDIT;
            case "apply", "ide-apply" -> ExecutionMode.IDE_APPLY;
            case "json", "json-api" -> ExecutionMode.JSON_API;
            case "review", "ci-review" -> ExecutionMode.CI_REVIEW;
            case "analysis" -> ExecutionMode.ANALYSIS;
            case "chat" -> ExecutionMode.CHAT;
            default -> {
                log.warn("Unknown execution mode suffix '{}', falling back to CHAT", suffix);
                yield ExecutionMode.CHAT;
            }
        };
    }
}
