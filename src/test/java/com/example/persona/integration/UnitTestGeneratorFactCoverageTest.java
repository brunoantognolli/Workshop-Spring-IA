package com.example.persona.integration;

import com.example.persona.examples.UnitTestGeneratorPersonaRunner;
import com.example.persona.examples.contracts.UnitTestGeneratorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: validates that the Unit Test Generator Persona output
 * conforms to the contract schema and is factually grounded in the source code.
 *
 * <p>Run with: {@code mvn test -Dgroups="integration"}
 */
@SpringBootTest
@Tag("integration")
class UnitTestGeneratorFactCoverageTest {

    private static final String SAMPLE_CLASS = """
            package com.example.shop;

            import java.util.List;
            import java.util.Optional;

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
                            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
                    order.cancel();
                    repository.save(order);
                }
            }
            """;

    @Autowired
    private UnitTestGeneratorPersonaRunner testGeneratorRunner;

    @Test
    @DisplayName("Generated test class name must be based on target class")
    void testClassNameShouldBeDerivedCorrectly() {
        UnitTestGeneratorOutput output = testGeneratorRunner.generateTests(SAMPLE_CLASS);

        assertThat(output.targetClassName())
                .as("Target class name must match exactly")
                .isEqualTo("OrderService");

        assertThat(output.testClassName())
                .as("Generated test class name should contain 'OrderService'")
                .contains("OrderService");
    }

    @Test
    @DisplayName("Generated output must define mocks for collaborators")
    void shouldDefineMocksForCollaborators() {
        UnitTestGeneratorOutput output = testGeneratorRunner.generateTests(SAMPLE_CLASS);

        assertThat(output.fields())
                .as("Must declare mocked collaborators")
                .isNotEmpty();

        // Verify that the fields contain repository and paymentGateway mocks
        boolean hasRepositoryMock = output.fields().stream()
                .anyMatch(field -> field.type().contains("OrderRepository") && field.annotations().contains("@Mock"));
        boolean hasPaymentGatewayMock = output.fields().stream()
                .anyMatch(field -> field.type().contains("PaymentGateway") && field.annotations().contains("@Mock"));
        boolean hasInjectMocks = output.fields().stream()
                .anyMatch(field -> field.type().contains("OrderService") && field.annotations().contains("@InjectMocks"));

        assertThat(hasRepositoryMock).as("Should mock OrderRepository").isTrue();
        assertThat(hasPaymentGatewayMock).as("Should mock PaymentGateway").isTrue();
        assertThat(hasInjectMocks).as("Should declare InjectMocks for OrderService").isTrue();
    }

    @Test
    @DisplayName("Generated output must contain test methods matching source methods")
    void shouldContainTestMethodsMatchingSource() {
        UnitTestGeneratorOutput output = testGeneratorRunner.generateTests(SAMPLE_CLASS);

        assertThat(output.testMethods())
                .as("Must generate test methods")
                .isNotEmpty();

        // Ensure tests exist for both business methods
        boolean hasCreateOrderTest = output.testMethods().stream()
                .anyMatch(m -> m.name().toLowerCase().contains("createorder"));
        boolean hasCancelOrderTest = output.testMethods().stream()
                .anyMatch(m -> m.name().toLowerCase().contains("cancelorder"));

        assertThat(hasCreateOrderTest).as("Should generate tests for createOrder").isTrue();
        assertThat(hasCancelOrderTest).as("Should generate tests for cancelOrder").isTrue();
    }

    @Test
    @DisplayName("Generated test methods should have JUnit 5 / AssertJ structures")
    void shouldHaveJUnit5AssertionsAndNoTodo() {
        UnitTestGeneratorOutput output = testGeneratorRunner.generateTests(SAMPLE_CLASS);

        assertThat(output.imports())
                .as("Should import JUnit 5 annotations")
                .contains("org.junit.jupiter.api.Test");

        assertThat(output.fullClassCode())
                .as("Generated test class code must not be blank")
                .isNotBlank();

        String fullCodeUpper = output.fullClassCode().toUpperCase();
        assertThat(fullCodeUpper)
                .as("Should not contain placeholder TODO or FIXME")
                .doesNotContain("TODO")
                .doesNotContain("FIXME");
    }

    @Test
    @DisplayName("Should refuse JUnit 4 generation and return unsupportedMessage when environment is legacy")
    void shouldRefuseJUnit4WhenEnvironmentIsLegacy() {
        // Simulating legacy Java 7 and Spring Boot 1.5.0 target environment
        UnitTestGeneratorOutput output = testGeneratorRunner.generateTests(SAMPLE_CLASS, "1.7", "1.5.0.RELEASE");

        assertThat(output.unsupportedMessage())
                .as("Should populate unsupported message warning when JUnit 5 is not supported")
                .isNotBlank()
                .containsIgnoringCase("not capable")
                .containsIgnoringCase("junit 4")
                .containsIgnoringCase("not permitted");

        assertThat(output.fullClassCode())
                .as("Should not generate any test code for unsupported target environment")
                .isNullOrEmpty();
    }
}
