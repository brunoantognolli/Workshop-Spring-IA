package com.example.persona.core;

import com.example.persona.contracts.OutputValidator;
import com.example.persona.contracts.ValidationResult;
import com.example.persona.guardrails.BoundaryAdvisor;
import com.example.persona.guardrails.BoundaryViolationException;
import com.example.persona.routing.FallbackTrigger;
import com.example.persona.skills.SkillRegistrar;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * Core orchestrator for running a {@link PersonaDefinition} against user input.
 *
 * <h3>Execution flow:</h3>
 * <ol>
 *   <li>Build system prompt from {@code role} + {@code boundaries}</li>
 *   <li>Register skills as LLM tools via {@link SkillRegistrar}</li>
 *   <li>Execute with the PRIMARY model (Ollama mistral:7b)</li>
 *   <li>Validate output against the declared {@code output_contract}</li>
 *   <li>If validation fails → escalate to FALLBACK cloud model</li>
 *   <li>Apply {@link BoundaryAdvisor} guardrails on every call</li>
 *   <li>Deserialize and return the typed output object</li>
 * </ol>
 */
@Component
public class PersonaEngine {

    private static final Logger log = LoggerFactory.getLogger(PersonaEngine.class);

    private final OllamaChatModel primaryModel;
    private final Optional<OpenAiChatModel> fallbackModel;
    private final SkillRegistrar skillRegistrar;
    private final OutputValidator outputValidator;
    private final ObjectMapper objectMapper;
    private final ChatMemory chatMemory;

    public PersonaEngine(OllamaChatModel primaryModel,
                         Optional<OpenAiChatModel> fallbackModel,
                         SkillRegistrar skillRegistrar,
                         OutputValidator outputValidator,
                         ChatMemory chatMemory) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.skillRegistrar = skillRegistrar;
        this.outputValidator = outputValidator;
        this.objectMapper = new ObjectMapper();
        this.chatMemory = chatMemory;
    }

    /**
     * Executes the persona against the given user input.
     *
     * @param persona   the fully-loaded persona definition
     * @param userInput raw input text (e.g., Java source code, ADR text)
     * @return deserialized output object matching the declared output contract schema
     */
    public Object execute(PersonaDefinition persona, String userInput) {
        return execute(persona, userInput, false);
    }

    public Object execute(PersonaDefinition persona, String userInput, boolean ignoreContract) {
        return execute(persona, userInput, ignoreContract, null, null);
    }

    public Object execute(PersonaDefinition persona, String userInput, boolean ignoreContract, String conversationId, String userId) {
        String formatInstructions = "";
        BeanOutputConverter<?> converter = null;
        Class<?> outputClass = null;

        if (!ignoreContract && persona.outputContract() != null) {
            outputClass = resolveOutputClass(persona.outputContract().schema());
            converter = new BeanOutputConverter<>(outputClass);
            formatInstructions = converter.getFormat();
        }

        String systemPrompt = buildSystemPrompt(persona, formatInstructions);
        List<ToolCallback> tools = skillRegistrar.registerSkills(persona.skills());
        BoundaryAdvisor advisor = new BoundaryAdvisor(persona.boundaries(), persona.guardrails());

        FallbackTrigger trigger = resolveTrigger(persona);

        // --- ALWAYS: skip primary and go straight to fallback ---
        if (trigger == FallbackTrigger.ALWAYS) {
            log.info("[{}] Trigger=ALWAYS — using cloud fallback directly", persona.id());
            return executeAndConvert(buildClientWithMemory(fallbackModel(), advisor, conversationId), systemPrompt, userInput, tools, converter, ignoreContract, conversationId);
        }

        // --- Try PRIMARY (Ollama) ---
        try {
            log.info("[{}] Calling primary model (Ollama mistral:7b)", persona.id());
            String rawResponse = callModel(buildClientWithMemory(primaryModel, advisor, conversationId), systemPrompt, userInput, tools, conversationId);

            if (ignoreContract || converter == null) {
                return rawResponse;
            }

            ValidationResult validation = outputValidator.validate(rawResponse, outputClass);
            if (validation.pass()) {
                log.info("[{}] Primary model: contract validation PASSED", persona.id());
                return converter.convert(rawResponse);
            }

            log.warn("[{}] Primary model: contract validation FAILED — {}", persona.id(), validation.errors());
            if (trigger == FallbackTrigger.CONTRACT_FAILURE) {
                return escalateToFallback(persona, systemPrompt, userInput, tools, converter, advisor, ignoreContract, conversationId);
            }
            return converter.convert(rawResponse);

        } catch (BoundaryViolationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[{}] Primary model threw exception: {}", persona.id(), e.getMessage());
            if (trigger == FallbackTrigger.ON_ERROR || trigger == FallbackTrigger.CONTRACT_FAILURE) {
                return escalateToFallback(persona, systemPrompt, userInput, tools, converter, advisor, ignoreContract, conversationId);
            }
            throw new PersonaExecutionException("Primary model failed with no fallback configured", e);
        }
    }

    /**
     * Executes the persona and streams the raw output bypassing runtime validation.
     */
    public Flux<String> streamUnvalidated(PersonaDefinition persona, String userInput) {
        return streamUnvalidated(persona, userInput, false);
    }

    public Flux<String> streamUnvalidated(PersonaDefinition persona, String userInput, boolean ignoreContract) {
        String formatInstructions = "";
        if (!ignoreContract && persona.outputContract() != null) {
            Class<?> outputClass = resolveOutputClass(persona.outputContract().schema());
            BeanOutputConverter<?> converter = new BeanOutputConverter<>(outputClass);
            formatInstructions = converter.getFormat();
        }

        String systemPrompt = buildSystemPrompt(persona, formatInstructions);
        List<ToolCallback> tools = skillRegistrar.registerSkills(persona.skills());
        BoundaryAdvisor advisor = new BoundaryAdvisor(persona.boundaries(), persona.guardrails());

        FallbackTrigger trigger = resolveTrigger(persona);

        if (trigger == FallbackTrigger.ALWAYS) {
            log.info("[{}] Trigger=ALWAYS — streaming from cloud fallback directly", persona.id());
            return streamModel(buildClient(fallbackModel(), advisor), systemPrompt, userInput, tools);
        }

        log.info("[{}] Streaming from primary model (Ollama)", persona.id());
        return streamModel(buildClient(primaryModel, advisor), systemPrompt, userInput, tools)
            .onErrorResume(e -> {
                log.warn("[{}] Primary model threw exception during stream: {}", persona.id(), e.getMessage());
                if (trigger == FallbackTrigger.ON_ERROR || trigger == FallbackTrigger.CONTRACT_FAILURE) {
                    log.info("[{}] Escalating to cloud fallback model for stream", persona.id());
                    return streamModel(buildClient(fallbackModel(), advisor), systemPrompt, userInput, tools);
                }
                return Flux.error(new PersonaExecutionException("Primary model failed with no fallback configured", e));
            });
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Object escalateToFallback(PersonaDefinition persona,
                                      String systemPrompt, String userInput,
                                      List<ToolCallback> tools,
                                      BeanOutputConverter<?> converter,
                                      BoundaryAdvisor advisor,
                                      boolean ignoreContract,
                                      String conversationId) {
        log.info("[{}] Escalating to cloud fallback model", persona.id());
        ChatClient fallbackClient = buildClientWithMemory(fallbackModel(), advisor, conversationId);
        return executeAndConvert(fallbackClient, systemPrompt, userInput, tools, converter, ignoreContract, conversationId);
    }

    private Object executeAndConvert(ChatClient client, String systemPrompt,
                                     String userInput, List<ToolCallback> tools,
                                     BeanOutputConverter<?> converter, boolean ignoreContract, String conversationId) {
        String raw = callModel(client, systemPrompt, userInput, tools, conversationId);
        if (ignoreContract || converter == null) {
            return raw;
        }
        return converter.convert(raw);
    }

    private String callModel(ChatClient client, String systemPrompt,
                              String userInput, List<ToolCallback> tools) {
        return callModel(client, systemPrompt, userInput, tools, null);
    }

    private String callModel(ChatClient client, String systemPrompt,
                              String userInput, List<ToolCallback> tools, String conversationId) {
        ChatClient.ChatClientRequestSpec spec = client.prompt()
                .system(systemPrompt)
                .user(userInput)
                .toolCallbacks(tools.toArray(ToolCallback[]::new));
        if (conversationId != null) {
            spec.advisors(a -> a.param("chat_memory_conversation_id", conversationId)
                                .param("chat_memory_retrieve_size", 10));
        }
        return spec.call().content();
    }

    private Flux<String> streamModel(ChatClient client, String systemPrompt,
                                      String userInput, List<ToolCallback> tools) {
        return client.prompt()
                .system(systemPrompt)
                .user(userInput)
                .toolCallbacks(tools.toArray(ToolCallback[]::new))
                .stream()
                .content();
    }

    private ChatClient buildClient(ChatModel model, BoundaryAdvisor advisor) {
        return ChatClient.builder(model).defaultAdvisors(advisor).build();
    }

    private ChatClient buildClientWithMemory(ChatModel model, BoundaryAdvisor advisor, String conversationId) {
        ChatClient.Builder builder = ChatClient.builder(model).defaultAdvisors(advisor);
        if (conversationId != null) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        return builder.build();
    }

    private OpenAiChatModel fallbackModel() {
        return fallbackModel.orElseThrow(() ->
                new PersonaExecutionException(
                        "Cloud fallback required but OPENAI_API_KEY is not configured. " +
                        "Set the OPENAI_API_KEY environment variable to enable fallback."));
    }

    private String buildSystemPrompt(PersonaDefinition persona, String formatInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Role\n").append(persona.role()).append("\n\n");

        if (persona.boundaries() != null && !persona.boundaries().isEmpty()) {
            sb.append("# Strict Boundaries\n");
            persona.boundaries().forEach(b -> sb.append("- ").append(b).append("\n"));
            sb.append("\n");
        }

        if (formatInstructions != null && !formatInstructions.isBlank()) {
            sb.append("# Output Format\n").append(formatInstructions);
        }
        return sb.toString();
    }

    private Class<?> resolveOutputClass(String fullyQualifiedName) {
        try {
            return Class.forName(fullyQualifiedName);
        } catch (ClassNotFoundException e) {
            throw new PersonaExecutionException(
                    "Output contract class not found: " + fullyQualifiedName +
                    ". Ensure the Java Record is on the classpath.", e);
        }
    }

    private FallbackTrigger resolveTrigger(PersonaDefinition persona) {
        if (persona.model() != null && persona.model().fallback() != null
                && persona.model().fallback().trigger() != null) {
            return persona.model().fallback().trigger();
        }
        return FallbackTrigger.CONTRACT_FAILURE;
    }

    /** Thrown when persona execution fails unrecoverably. */
    public static class PersonaExecutionException extends RuntimeException {
        public PersonaExecutionException(String message) { super(message); }
        public PersonaExecutionException(String message, Throwable cause) { super(message, cause); }
    }
}
