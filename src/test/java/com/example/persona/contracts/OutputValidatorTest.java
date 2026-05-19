package com.example.persona.contracts;

import com.example.persona.examples.contracts.JavaDocOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link OutputValidator}.
 * Uses JSON fixtures — no LLM calls required.
 */
class OutputValidatorTest {

    private OutputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OutputValidator();
    }

    private static final String VALID_JAVADOC_JSON = """
            {
              "className": "OrderService",
              "summary": "Manages the order lifecycle.",
              "methods": [
                {
                  "name": "createOrder",
                  "signature": "public Order createOrder(long customerId, List<OrderItem> items)",
                  "description": "Creates a new order for the given customer.",
                  "params": ["customerId - the customer identifier", "items - list of order items"],
                  "returns": "the created Order"
                }
              ],
              "complexity": {
                "overall": "low",
                "suggestions": ["Consider extracting validation logic"]
              }
            }
            """;

    private static final String MISSING_REQUIRED_FIELD_JSON = """
            {
              "className": "OrderService"
            }
            """;

    private static final String NOT_JSON = "This is not JSON at all";

    private static final String JSON_IN_MARKDOWN_FENCE = """
            ```json
            {
              "className": "OrderService",
              "summary": "Manages orders.",
              "methods": [],
              "complexity": { "overall": "low", "suggestions": [] }
            }
            ```
            """;

    @Test
    @DisplayName("Should pass valid JSON matching JavaDocOutput schema")
    void shouldPassValidJson() {
        ValidationResult result = validator.validate(VALID_JAVADOC_JSON, JavaDocOutput.class);
        assertThat(result.pass()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("Should fail when required fields are missing")
    void shouldFailMissingRequiredFields() {
        ValidationResult result = validator.validate(MISSING_REQUIRED_FIELD_JSON, JavaDocOutput.class);
        assertThat(result.pass()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail when response is not JSON")
    void shouldFailNonJsonResponse() {
        ValidationResult result = validator.validate(NOT_JSON, JavaDocOutput.class);
        assertThat(result.pass()).isFalse();
    }

    @Test
    @DisplayName("Should fail when response is null or blank")
    void shouldFailNullOrBlankResponse() {
        assertThat(validator.validate(null, JavaDocOutput.class).pass()).isFalse();
        assertThat(validator.validate("   ", JavaDocOutput.class).pass()).isFalse();
    }

    @Test
    @DisplayName("Should extract JSON from markdown code fence")
    void shouldExtractJsonFromMarkdownFence() {
        ValidationResult result = validator.validate(JSON_IN_MARKDOWN_FENCE, JavaDocOutput.class);
        assertThat(result.pass()).isTrue();
    }
}
