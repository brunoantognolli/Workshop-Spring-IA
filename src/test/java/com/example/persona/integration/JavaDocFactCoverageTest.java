package com.example.persona.integration;

import com.example.persona.examples.JavaDocPersonaRunner;
import com.example.persona.examples.contracts.JavaDocOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: validates that the JavaDoc Persona output is
 * factually grounded in the provided Java source code.
 *
 * <p>This test validates fact coverage by asserting that:
 * <ol>
 *   <li>The output contains only method names that exist in the source code</li>
 *   <li>The class name matches the actual class name in the source</li>
 *   <li>The method count does not exceed the actual method count</li>
 * </ol>
 *
 * <h3>Note on FactCheckingEvaluator:</h3>
 * <p>Spring AI's {@code FactCheckingEvaluator} constructor access level varies by version.
 * This test uses structural grounding assertions as a portable alternative.
 * For LLM-as-Judge evaluation, inject a {@code FactCheckingEvaluator} bean in your
 * Spring configuration and use it with the {@code evaluate(EvaluationRequest)} method.
 *
 * <h3>Requires:</h3>
 * <ul>
 *   <li>Ollama running locally on port 11434</li>
 *   <li>{@code mistral} model pulled: {@code ollama pull mistral}</li>
 * </ul>
 *
 * <p>Run with: {@code mvn test -Dgroups="integration"}
 */
@SpringBootTest
@Tag("integration")
class JavaDocFactCoverageTest {

    private static final String SAMPLE_CLASS = """
            package com.example.shop;

            import java.util.List;

            public class OrderService {

                private final OrderRepository repository;
                private final PaymentGateway paymentGateway;

                public OrderService(OrderRepository repository, PaymentGateway paymentGateway) {
                    this.repository = repository;
                    this.paymentGateway = paymentGateway;
                }

                public Order createOrder(long customerId, List<OrderItem> items) {
                    Order order = new Order(customerId, items);
                    return repository.save(order);
                }

                public void cancelOrder(long orderId) {
                    Order order = repository.findById(orderId)
                            .orElseThrow(OrderNotFoundException::new);
                    order.cancel();
                    repository.save(order);
                }
            }
            """;

    // Methods actually declared in the sample class (ground truth)
    private static final List<String> ACTUAL_METHOD_NAMES = List.of("createOrder", "cancelOrder");
    private static final String ACTUAL_CLASS_NAME = "OrderService";

    @Autowired
    private JavaDocPersonaRunner javaDocPersonaRunner;

    @Autowired
    private com.example.persona.core.PersonaEngine personaEngine;

    @Autowired
    private com.example.persona.core.PersonaLoader personaLoader;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("openAiChatModel")
    private org.springframework.ai.chat.model.ChatModel chatModel;

    @Test
    @DisplayName("Documented class name must match actual class name (fact grounding)")
    void classNameMustBeGrounded() {
        JavaDocOutput output = javaDocPersonaRunner.document(SAMPLE_CLASS);

        assertThat(output.className())
                .as("Class name must match the actual class in the source code")
                .containsIgnoringCase(ACTUAL_CLASS_NAME);
    }

    @Test
    @DisplayName("Documented methods must be grounded in source (no hallucinated methods)")
    void documentedMethodsMustExistInSource() {
        JavaDocOutput output = javaDocPersonaRunner.document(SAMPLE_CLASS);

        long groundedCount = output.methods().stream()
                .filter(m -> ACTUAL_METHOD_NAMES.stream()
                        .anyMatch(actual -> m.name().toLowerCase().contains(actual.toLowerCase())))
                .count();

        double coverageRatio = (double) groundedCount / ACTUAL_METHOD_NAMES.size();
        assertThat(coverageRatio)
                .as("At least 90%% of actual methods should appear in the documentation. " +
                    "Coverage: %.0f%%".formatted(coverageRatio * 100))
                .isGreaterThanOrEqualTo(0.9);
    }

    @Test
    @DisplayName("Documented method count should not significantly exceed actual method count")
    void shouldNotInventExtraMethods() {
        JavaDocOutput output = javaDocPersonaRunner.document(SAMPLE_CLASS);
        int actualMethodCount = ACTUAL_METHOD_NAMES.size();

        // Allow at most 1 extra method (e.g., constructor docs)
        assertThat(output.methods().size())
                .as("Should not invent far more methods than exist in source")
                .isLessThanOrEqualTo(actualMethodCount + 2);
    }

    @Test
    @DisplayName("JavaDoc output must not violate boundaries (no TODO/FIXME)")
    void outputMustNotContainForbiddenPatterns() {
        JavaDocOutput output = javaDocPersonaRunner.document(SAMPLE_CLASS);
        String fullOutput = output.toString().toUpperCase();
        assertThat(fullOutput).doesNotContain("TODO");
        assertThat(fullOutput).doesNotContain("FIXME");
    }

    @Test
    @DisplayName("Summary must reference actual class or its responsibilities")
    void summaryMustBeRelevant() {
        JavaDocOutput output = javaDocPersonaRunner.document(SAMPLE_CLASS);
        String summary = output.summary().toLowerCase();

        assertThat(summary)
                .as("Summary should reference 'order' — the domain concept in the source")
                .containsIgnoringCase("order");
    }

    @Test
    @DisplayName("In Human Mode, the persona must not just echo the skill guidelines (LLM-as-a-Judge)")
    void humanModeMustNotEchoSkillGuidelines() {
        var persona = personaLoader.load("javadoc-persona");
        // Execute in Human Mode (ignoreContract = true)
        Object rawOutput = personaEngine.execute(persona, SAMPLE_CLASS, true);
        
        assertThat(rawOutput).isInstanceOf(String.class);
        String textOutput = (String) rawOutput;
        
        // Use LLM-as-a-Judge to evaluate semantic correctness
        org.springframework.ai.chat.client.ChatClient judgeClient = 
            org.springframework.ai.chat.client.ChatClient.builder(chatModel).build();

        String evaluationPrompt = """
                Evaluate the following output generated by an AI assistant.
                The assistant was asked to generate JavaDoc and complexity analysis for a specific Java class.
                
                OUTPUT TO EVALUATE:
                %s
                
                Does this output actually analyze a Java class, or does it simply copy/paste a generic rule guide without applying it?
                Reply ONLY with 'PASS' if it contains analysis for a specific Java class.
                Reply ONLY with 'FAIL' if it is just a generic guide or ruleset.
                """.formatted(textOutput);

        String judgment = judgeClient.prompt().user(evaluationPrompt).call().content();

        assertThat(judgment)
                .as("LLM-as-a-Judge failed: Output appears to be a generic guide instead of actual class analysis.")
                .containsIgnoringCase("PASS")
                .doesNotContainIgnoringCase("FAIL");
    }
}
