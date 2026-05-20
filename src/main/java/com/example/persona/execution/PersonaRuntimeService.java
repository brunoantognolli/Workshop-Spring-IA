package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Central orchestrator for adapter-aware persona execution.
 *
 * <h3>Execution pipeline:</h3>
 * <ol>
 *   <li>Parse model name → {@link ModelNameParser.ParsedModelName}</li>
 *   <li>Load {@link PersonaDefinition} via {@link PersonaLoader}</li>
 *   <li>Build {@link ExecutionContext} with safe defaults</li>
 *   <li>Resolve {@link ExecutionAdapter} via {@link ExecutionAdapterRegistry}</li>
 *   <li>Build system prompt via {@link PersonaEngine}</li>
 *   <li>Adapt prompt via the resolved adapter</li>
 *   <li>Call LLM via PersonaEngine (with ignoreContract=true for IDE modes)</li>
 *   <li>Adapt response via the resolved adapter</li>
 *   <li>Return the final response</li>
 * </ol>
 *
 * <p>This service does NOT replace {@link PersonaEngine}. It wraps and
 * orchestrates it, adding the adapter layer on top. PersonaEngine remains
 * responsible for model selection, fallback, memory, and guardrails.
 */
@Service
public class PersonaRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(PersonaRuntimeService.class);

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;
    private final ExecutionAdapterRegistry adapterRegistry;

    public PersonaRuntimeService(PersonaLoader personaLoader,
                                  PersonaEngine personaEngine,
                                  ExecutionAdapterRegistry adapterRegistry) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * Executes a persona with execution adapter support.
     *
     * @param modelName the full model name (e.g., "javadoc-persona:edit")
     * @param userInput the user's input/instruction
     * @return the adapted response string
     */
    public String execute(String modelName, String userInput) {
        // 1. Parse model name
        ModelNameParser.ParsedModelName parsed = ModelNameParser.parse(modelName);
        log.info("Executing persona '{}' with mode {}", parsed.personaId(), parsed.executionMode());

        // 2. Load persona
        PersonaDefinition persona = personaLoader.load(parsed.personaId());

        // 3. Build execution context
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId(parsed.personaId())
                .personaVersion(persona.version())
                .executionMode(parsed.executionMode())
                .userInstruction(userInput)
                .applyPersonaConfig(persona)
                .build();

        // 4. Resolve adapter
        ExecutionAdapter adapter = adapterRegistry.resolve(context);
        log.debug("Using adapter: {}", adapter.getClass().getSimpleName());

        // 5-6. Build and adapt system prompt
        boolean shouldIgnoreContract = shouldIgnoreContract(context);
        String baseSystemPrompt = personaEngine.buildSystemPrompt(persona,
                shouldIgnoreContract ? "" : getFormatInstructions(persona));
        String adaptedPrompt = adapter.adaptPrompt(persona, context, baseSystemPrompt);

        // 7. Execute via PersonaEngine with the adapter-built system prompt
        Object rawResult = personaEngine.executeWithSystemPrompt(persona, userInput,
                shouldIgnoreContract, adaptedPrompt);

        String rawResponse = rawResult instanceof String
                ? (String) rawResult
                : rawResult.toString();

        // 8. Adapt response
        String adaptedResponse = adapter.adaptResponse(persona, context, rawResponse);

        log.info("Persona '{}' execution complete (mode={}, adapter={})",
                parsed.personaId(), context.executionMode(), adapter.getClass().getSimpleName());
        return adaptedResponse;
    }

    /**
     * Streams a persona execution with adapter prompt adaptation.
     *
     * <p>Streaming applies prompt adaptation only — response post-processing
     * is not applied to individual tokens. This is suitable for Continue.dev
     * and similar clients that handle code formatting on their end.
     *
     * @param modelName the full model name (e.g., "javadoc-persona:edit")
     * @param userInput the user's input/instruction
     * @return flux of response tokens
     */
    public Flux<String> stream(String modelName, String userInput) {
        ModelNameParser.ParsedModelName parsed = ModelNameParser.parse(modelName);
        log.info("Streaming persona '{}' with mode {}", parsed.personaId(), parsed.executionMode());

        PersonaDefinition persona = personaLoader.load(parsed.personaId());

        ExecutionContext context = new ExecutionContextBuilder()
                .personaId(parsed.personaId())
                .personaVersion(persona.version())
                .executionMode(parsed.executionMode())
                .userInstruction(userInput)
                .applyPersonaConfig(persona)
                .build();

        ExecutionAdapter adapter = adapterRegistry.resolve(context);
        boolean shouldIgnoreContract = shouldIgnoreContract(context);

        // For streaming, we adapt the prompt but stream the response directly
        String baseSystemPrompt = personaEngine.buildSystemPrompt(persona,
                shouldIgnoreContract ? "" : getFormatInstructions(persona));
        String adaptedPrompt = adapter.adaptPrompt(persona, context, baseSystemPrompt);

        // Stream tokens immediately — Continue/Cursor cancel the HTTP connection if no chunks arrive
        // for several seconds (buffering the full LLM response breaks inline Edit diffs).
        return personaEngine.streamWithSystemPrompt(persona, userInput,
                shouldIgnoreContract, adaptedPrompt);
    }

    /**
     * Returns the execution mode for a given model name, useful for
     * the API layer to determine behavior.
     */
    public ExecutionMode resolveExecutionMode(String modelName) {
        return ModelNameParser.parse(modelName).executionMode();
    }

    /**
     * Returns the persona ID extracted from a model name.
     */
    public String resolvePersonaId(String modelName) {
        return ModelNameParser.parse(modelName).personaId();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * IDE modes should ignore the persona's JSON output contract.
     */
    private boolean shouldIgnoreContract(ExecutionContext context) {
        return context.executionMode() == ExecutionMode.IDE_EDIT
                || context.executionMode() == ExecutionMode.IDE_APPLY
                || context.executionMode() == ExecutionMode.CHAT
                || context.executionMode() == ExecutionMode.ANALYSIS
                || context.executionMode() == ExecutionMode.CI_REVIEW;
    }

    /**
     * Gets format instructions from the persona's output contract, if any.
     */
    private String getFormatInstructions(PersonaDefinition persona) {
        if (persona.outputContract() == null) {
            return "";
        }
        try {
            Class<?> outputClass = Class.forName(persona.outputContract().schema());
            var converter = new org.springframework.ai.converter.BeanOutputConverter<>(outputClass);
            return converter.getFormat();
        } catch (ClassNotFoundException e) {
            log.warn("Could not load output contract class: {}", persona.outputContract().schema());
            return "";
        }
    }
}
