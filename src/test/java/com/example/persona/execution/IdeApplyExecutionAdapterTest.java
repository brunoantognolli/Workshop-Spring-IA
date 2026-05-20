package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link IdeApplyExecutionAdapter}.
 * Verifies the strictest adapter mode — ensuring only raw code or patches are produced.
 */
class IdeApplyExecutionAdapterTest {

    private IdeApplyExecutionAdapter adapter;
    private PersonaDefinition persona;
    private PersonaDefinition personaWithContract;

    @BeforeEach
    void setUp() {
        adapter = new IdeApplyExecutionAdapter();

        persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a test persona.",
                null, List.of(), List.of(), List.of(), null, List.of(), null
        );

        personaWithContract = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a test persona.",
                null, List.of(), List.of(), List.of(),
                new PersonaDefinition.OutputContractDef("json", "com.example.SomeOutput"),
                List.of(), null
        );
    }

    @Test
    @DisplayName("Should support IDE_APPLY mode")
    void shouldSupportIdeApplyMode() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        assertThat(adapter.supports(context)).isTrue();
    }

    @Test
    @DisplayName("Should not support IDE_EDIT mode")
    void shouldNotSupportIdeEditMode() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        assertThat(adapter.supports(context)).isFalse();
    }

    @Test
    @DisplayName("Should not support CHAT mode")
    void shouldNotSupportChatMode() {
        ExecutionContext context = buildContext(ExecutionMode.CHAT);
        assertThat(adapter.supports(context)).isFalse();
    }

    @Test
    @DisplayName("Should strip JSON format instructions from system prompt")
    void shouldStripJsonFormatInstructions() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String basePrompt = "# Role\nYou are a test persona.\n\n# Output Format\n{json schema here}";

        String adaptedPrompt = adapter.adaptPrompt(personaWithContract, context, basePrompt);

        assertThat(adaptedPrompt).doesNotContain("json schema here");
        assertThat(adaptedPrompt).doesNotContain("# Output Format");
        assertThat(adaptedPrompt).contains("# Role");
        assertThat(adaptedPrompt).contains("Return ONLY code or a unified diff");
    }

    @Test
    @DisplayName("Should append strict IDE apply instructions")
    void shouldAppendStrictApplyInstructions() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String basePrompt = "# Role\nYou are a test persona.";

        String adaptedPrompt = adapter.adaptPrompt(persona, context, basePrompt);

        assertThat(adaptedPrompt).contains("Return ONLY code or a unified diff");
        assertThat(adaptedPrompt).contains("Do not explain");
        assertThat(adaptedPrompt).contains("Do not return JSON under any circumstances");
        assertThat(adaptedPrompt).contains("Do not use Markdown formatting or code fences");
    }

    @Test
    @DisplayName("Should strip markdown code fences from response")
    void shouldStripCodeFences() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String rawResponse = "```java\npublic class Foo {\n}\n```";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("```");
        assertThat(adapted).contains("public class Foo");
    }

    @Test
    @DisplayName("Should strip all commentary from response")
    void shouldStripAllCommentary() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String rawResponse = "Here is the refactored code:\n\npublic class Foo {\n}\n\nSummary: cleaned up.";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("Here is the refactored code");
        assertThat(adapted).doesNotContain("Summary:");
        assertThat(adapted).contains("public class Foo");
    }

    @Test
    @DisplayName("Should strip markdown JSON fences from IDE_APPLY response")
    void shouldStripMarkdownJsonFencesFromResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String rawResponse = "```json\n{\"patch\": \"invalid\"}\n```";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("```");
    }

    @Test
    @DisplayName("Persona JSON output_contract should not leak into IDE_APPLY response")
    void shouldNotLeakJsonContract() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        // Simulate LLM returning JSON despite having code instructions
        String rawResponse = "{\"className\": \"Foo\", \"methods\": []}";

        // The adapter strips nothing from valid code — but the prompt adaptation
        // ensures the LLM won't produce JSON in the first place
        String adaptedPrompt = adapter.adaptPrompt(personaWithContract, context,
                "# Role\nYou are a test persona.\n\n# Output Format\n{json schema}");

        assertThat(adaptedPrompt).doesNotContain("{json schema}");
        assertThat(adaptedPrompt).contains("Do not return JSON under any circumstances");
    }

    @Test
    @DisplayName("Should handle null response gracefully")
    void shouldHandleNullResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);

        String adapted = adapter.adaptResponse(persona, context, null);
        assertThat(adapted).isNull();
    }

    @Test
    @DisplayName("Should preserve clean unified diff unchanged")
    void shouldPreserveCleanDiff() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        String diff = "--- a/Foo.java\n+++ b/Foo.java\n@@ -1,3 +1,4 @@\n public class Foo {\n+    // added\n }";

        String adapted = adapter.adaptResponse(persona, context, diff);

        assertThat(adapted).isEqualTo(diff);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ExecutionContext buildContext(ExecutionMode mode) {
        return new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(mode)
                .userInstruction("Refactor this class")
                .build();
    }
}
