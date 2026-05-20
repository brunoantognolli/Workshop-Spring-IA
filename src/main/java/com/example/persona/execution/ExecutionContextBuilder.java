package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;

import java.util.Map;

/**
 * Builds an {@link ExecutionContext} from a parsed model name, persona definition,
 * and request metadata.
 *
 * <p>Applies safe defaults based on the {@link ExecutionMode} when the persona
 * YAML does not include an {@code execution:} configuration section.
 *
 * <h3>Default mappings:</h3>
 * <ul>
 *   <li>{@code CHAT}      → markdown allowed, JSON allowed, natural language</li>
 *   <li>{@code IDE_EDIT}  → no markdown, no JSON, full file output</li>
 *   <li>{@code IDE_APPLY} → no markdown, no JSON, patch-only output</li>
 *   <li>{@code JSON_API}  → no markdown, JSON required, structured output</li>
 * </ul>
 */
public class ExecutionContextBuilder {

    private String personaId;
    private String personaVersion;
    private ExecutionMode executionMode = ExecutionMode.CHAT;
    private ClientType clientType = ClientType.UNKNOWN;
    private String targetLanguage;
    private String activeFileName;
    private String activeFileContent;
    private String userInstruction;
    private OutputStrategy outputStrategy;
    private Boolean shouldReturnMarkdown;
    private Boolean shouldReturnJson;
    private Boolean shouldReturnFullFile;
    private Boolean shouldReturnPatch;

    public ExecutionContextBuilder personaId(String personaId) {
        this.personaId = personaId;
        return this;
    }

    public ExecutionContextBuilder personaVersion(String personaVersion) {
        this.personaVersion = personaVersion;
        return this;
    }

    public ExecutionContextBuilder executionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    public ExecutionContextBuilder clientType(ClientType clientType) {
        this.clientType = clientType;
        return this;
    }

    public ExecutionContextBuilder targetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
        return this;
    }

    public ExecutionContextBuilder activeFileName(String activeFileName) {
        this.activeFileName = activeFileName;
        return this;
    }

    public ExecutionContextBuilder activeFileContent(String activeFileContent) {
        this.activeFileContent = activeFileContent;
        return this;
    }

    public ExecutionContextBuilder userInstruction(String userInstruction) {
        this.userInstruction = userInstruction;
        return this;
    }

    public ExecutionContextBuilder outputStrategy(OutputStrategy outputStrategy) {
        this.outputStrategy = outputStrategy;
        return this;
    }

    public ExecutionContextBuilder shouldReturnMarkdown(boolean shouldReturnMarkdown) {
        this.shouldReturnMarkdown = shouldReturnMarkdown;
        return this;
    }

    public ExecutionContextBuilder shouldReturnJson(boolean shouldReturnJson) {
        this.shouldReturnJson = shouldReturnJson;
        return this;
    }

    public ExecutionContextBuilder shouldReturnFullFile(boolean shouldReturnFullFile) {
        this.shouldReturnFullFile = shouldReturnFullFile;
        return this;
    }

    public ExecutionContextBuilder shouldReturnPatch(boolean shouldReturnPatch) {
        this.shouldReturnPatch = shouldReturnPatch;
        return this;
    }

    /**
     * Applies persona-level execution configuration overrides, if present.
     *
     * <p>Reads the {@code execution.adapters.continue.edit} or
     * {@code execution.adapters.continue.apply} section from the persona YAML
     * and applies matching overrides to this builder.
     *
     * @param persona the loaded persona definition
     * @return this builder for chaining
     */
    public ExecutionContextBuilder applyPersonaConfig(PersonaDefinition persona) {
        if (persona.execution() == null) {
            return this;
        }

        PersonaDefinition.ExecutionConfig execConfig = persona.execution();

        // Apply adapter-specific config if available
        if (execConfig.adapters() != null) {
            Map<String, PersonaDefinition.AdapterConfig> continueConfig =
                    execConfig.adapters().get("continue");

            if (continueConfig != null) {
                String modeKey = resolveAdapterConfigKey(executionMode);
                PersonaDefinition.AdapterConfig adapterConfig = continueConfig.get(modeKey);

                if (adapterConfig != null) {
                    applyAdapterConfig(adapterConfig);
                }
            }
        }

        return this;
    }

    /**
     * Builds the {@link ExecutionContext}, applying safe defaults for any
     * values not explicitly set.
     */
    public ExecutionContext build() {
        OutputStrategy resolvedStrategy = outputStrategy != null
                ? outputStrategy : defaultOutputStrategy(executionMode);

        boolean resolvedMarkdown = shouldReturnMarkdown != null
                ? shouldReturnMarkdown : defaultShouldReturnMarkdown(executionMode);

        boolean resolvedJson = shouldReturnJson != null
                ? shouldReturnJson : defaultShouldReturnJson(executionMode);

        boolean resolvedFullFile = shouldReturnFullFile != null
                ? shouldReturnFullFile : defaultShouldReturnFullFile(executionMode);

        boolean resolvedPatch = shouldReturnPatch != null
                ? shouldReturnPatch : defaultShouldReturnPatch(executionMode);

        return new ExecutionContext(
                personaId,
                personaVersion,
                executionMode,
                clientType,
                targetLanguage,
                activeFileName,
                activeFileContent,
                userInstruction,
                resolvedStrategy,
                resolvedMarkdown,
                resolvedJson,
                resolvedFullFile,
                resolvedPatch
        );
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void applyAdapterConfig(PersonaDefinition.AdapterConfig config) {
        if (config.outputStrategy() != null) {
            this.outputStrategy = OutputStrategy.valueOf(config.outputStrategy());
        }
        this.shouldReturnMarkdown = config.allowMarkdown();
        this.shouldReturnJson = config.allowJson();
        // includeExplanation maps to markdown for now
    }

    private String resolveAdapterConfigKey(ExecutionMode mode) {
        return switch (mode) {
            case IDE_EDIT -> "edit";
            case IDE_APPLY -> "apply";
            case CHAT, ANALYSIS -> "chat";
            case JSON_API -> "json";
            case CI_REVIEW -> "review";
        };
    }

    static OutputStrategy defaultOutputStrategy(ExecutionMode mode) {
        return switch (mode) {
            case CHAT, ANALYSIS, CI_REVIEW -> OutputStrategy.NATURAL_LANGUAGE;
            case IDE_EDIT -> OutputStrategy.FULL_FILE;
            case IDE_APPLY -> OutputStrategy.PATCH_ONLY;
            case JSON_API -> OutputStrategy.STRUCTURED_JSON;
        };
    }

    private static boolean defaultShouldReturnMarkdown(ExecutionMode mode) {
        return switch (mode) {
            case CHAT, ANALYSIS, CI_REVIEW -> true;
            case IDE_EDIT, IDE_APPLY, JSON_API -> false;
        };
    }

    private static boolean defaultShouldReturnJson(ExecutionMode mode) {
        return switch (mode) {
            case JSON_API -> true;
            case CHAT, ANALYSIS, IDE_EDIT, IDE_APPLY, CI_REVIEW -> false;
        };
    }

    private static boolean defaultShouldReturnFullFile(ExecutionMode mode) {
        return switch (mode) {
            case IDE_EDIT -> true;
            case CHAT, ANALYSIS, IDE_APPLY, CI_REVIEW, JSON_API -> false;
        };
    }

    private static boolean defaultShouldReturnPatch(ExecutionMode mode) {
        return switch (mode) {
            case IDE_APPLY -> true;
            case CHAT, ANALYSIS, IDE_EDIT, CI_REVIEW, JSON_API -> false;
        };
    }
}
