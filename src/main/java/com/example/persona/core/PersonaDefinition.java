package com.example.persona.core;

import com.example.persona.routing.FallbackTrigger;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Immutable data model representing a fully parsed Persona definition.
 * Populated by {@link PersonaLoader} from a YAML file.
 *
 * <p>Adding a new persona requires ONLY a new YAML file + a Java Record for the
 * output contract. Zero engine changes needed.
 *
 * <p>The optional {@code execution} field allows per-persona configuration of
 * execution adapters (e.g., Continue.dev edit/apply modes).
 */
public record PersonaDefinition(
        String id,
        String version,
        String role,
        @JsonProperty("model") ModelConfig model,
        List<SkillDef> skills,
        List<String> boundaries,
        List<GuardrailDef> guardrails,
        @JsonProperty("output_contract") OutputContractDef outputContract,
        List<TestDef> tests,
        ExecutionConfig execution
) {

    /** Configuration for primary and fallback LLM models. */
    public record ModelConfig(
            ModelDef primary,
            ModelDef fallback
    ) {}

    /** A single model definition (provider + model name + optional timeout). */
    public record ModelDef(
            String provider,
            String model,
            String timeout,
            FallbackTrigger trigger
    ) {
        public ModelDef {
            // Default trigger when not specified in YAML
            if (trigger == null) trigger = FallbackTrigger.CONTRACT_FAILURE;
        }
    }

    /** Reference to a Markdown skill file on the classpath. */
    public record SkillDef(
            String id,
            String path
    ) {}

    /** A guardrail rule: type + pattern to detect in responses. */
    public record GuardrailDef(
            String type,
            String rule
    ) {}

    /** Output format and fully-qualified Java Record class name. */
    public record OutputContractDef(
            String format,
            String schema
    ) {}

    /** A test definition attached to this persona. */
    public record TestDef(
            String type,
            double threshold
    ) {}

    /**
     * Optional execution adapter configuration.
     * Maps the {@code execution:} section in the persona YAML.
     *
     * @param defaultMode    the default execution mode (e.g., "CHAT")
     * @param supportedModes list of supported execution modes
     * @param adapters       per-client adapter configurations (e.g., "continue" → { "edit" → config })
     */
    public record ExecutionConfig(
            String defaultMode,
            List<String> supportedModes,
            Map<String, Map<String, AdapterConfig>> adapters
    ) {}

    /**
     * Configuration for a specific adapter within a client context.
     * Maps entries like {@code execution.adapters.continue.edit}.
     *
     * @param outputStrategy    the output format (e.g., "FULL_FILE", "UNIFIED_DIFF")
     * @param allowMarkdown     whether markdown is allowed in the output
     * @param allowJson         whether JSON is allowed in the output
     * @param includeExplanation whether explanatory text is allowed
     */
    public record AdapterConfig(
            String outputStrategy,
            boolean allowMarkdown,
            boolean allowJson,
            boolean includeExplanation
    ) {}
}
