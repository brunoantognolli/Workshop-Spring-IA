package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChatExecutionAdapter}.
 */
class ChatExecutionAdapterTest {

    private ChatExecutionAdapter adapter;
    private PersonaDefinition persona;

    @BeforeEach
    void setUp() {
        adapter = new ChatExecutionAdapter();
        persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a helpful assistant.",
                null, List.of(), List.of(), List.of(), null, List.of(), null
        );
    }

    @Test
    @DisplayName("Should support CHAT, ANALYSIS, and CI_REVIEW modes")
    void shouldSupportNaturalLanguageModes() {
        assertThat(adapter.supports(context(ExecutionMode.CHAT))).isTrue();
        assertThat(adapter.supports(context(ExecutionMode.ANALYSIS))).isTrue();
        assertThat(adapter.supports(context(ExecutionMode.CI_REVIEW))).isTrue();
    }

    @Test
    @DisplayName("Should not support IDE_EDIT mode")
    void shouldNotSupportIdeEditMode() {
        assertThat(adapter.supports(context(ExecutionMode.IDE_EDIT))).isFalse();
    }

    @Test
    @DisplayName("Should pass through system prompt unchanged")
    void shouldPassThroughPrompt() {
        String basePrompt = "# Role\nYou are a helpful assistant.\n\nSome **markdown** rules.";

        String adapted = adapter.adaptPrompt(persona, context(ExecutionMode.CHAT), basePrompt);

        assertThat(adapted).isEqualTo(basePrompt);
    }

    @Test
    @DisplayName("Should pass through natural language response unchanged")
    void shouldPassThroughNaturalLanguageResponse() {
        String raw = """
                Here is my analysis:

                - Point one
                - Point two

                ```java
                public class Example {}
                ```
                """;

        String adapted = adapter.adaptResponse(persona, context(ExecutionMode.CHAT), raw);

        assertThat(adapted).isEqualTo(raw);
    }

    private ExecutionContext context(ExecutionMode mode) {
        return new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(mode)
                .userInstruction("Explain this code")
                .build();
    }
}
