package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonApiExecutionAdapter}.
 */
class JsonApiExecutionAdapterTest {

    private JsonApiExecutionAdapter adapter;
    private PersonaDefinition persona;

    @BeforeEach
    void setUp() {
        adapter = new JsonApiExecutionAdapter();
        persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a structured API persona.",
                null, List.of(), List.of(), List.of(),
                new PersonaDefinition.OutputContractDef("json", "com.example.SomeOutput"),
                List.of(), null
        );
    }

    @Test
    @DisplayName("Should support JSON_API mode only")
    void shouldSupportJsonApiMode() {
        assertThat(adapter.supports(context(ExecutionMode.JSON_API))).isTrue();
        assertThat(adapter.supports(context(ExecutionMode.CHAT))).isFalse();
        assertThat(adapter.supports(context(ExecutionMode.IDE_EDIT))).isFalse();
    }

    @Test
    @DisplayName("Should append JSON API instructions to system prompt")
    void shouldAppendJsonApiInstructions() {
        String basePrompt = "# Role\nYou are a structured API persona.";

        String adapted = adapter.adaptPrompt(persona, context(ExecutionMode.JSON_API), basePrompt);

        assertThat(adapted).startsWith(basePrompt);
        assertThat(adapted).contains("JSON API");
        assertThat(adapted).contains("valid JSON");
        assertThat(adapted).contains("Do not wrap the JSON in Markdown code fences");
    }

    @Test
    @DisplayName("Should extract JSON from markdown code fences")
    void shouldExtractJsonFromMarkdownFences() {
        String raw = """
                ```json
                {"summary": "ok", "score": 0.9}
                ```
                """;

        String adapted = adapter.adaptResponse(persona, context(ExecutionMode.JSON_API), raw);

        assertThat(adapted).doesNotContain("```");
        assertThat(adapted).isEqualTo("{\"summary\": \"ok\", \"score\": 0.9}");
    }

    @Test
    @DisplayName("Should leave bare JSON response unchanged")
    void shouldLeaveBareJsonUnchanged() {
        String raw = "{\"summary\": \"ok\", \"items\": []}";

        String adapted = adapter.adaptResponse(persona, context(ExecutionMode.JSON_API), raw);

        assertThat(adapted).isEqualTo(raw);
    }

    @Test
    @DisplayName("Should preserve parseable JSON after adaptation")
    void shouldPreserveParseableJson() throws Exception {
        String raw = "```json\n{\"status\": \"PASS\"}\n```";

        String adapted = adapter.adaptResponse(persona, context(ExecutionMode.JSON_API), raw);

        new com.fasterxml.jackson.databind.ObjectMapper().readTree(adapted);
        assertThat(adapted).contains("\"status\"");
    }

    private ExecutionContext context(ExecutionMode mode) {
        return new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(mode)
                .userInstruction("Analyze this")
                .build();
    }
}
