package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link IdeEditExecutionAdapter}.
 * Verifies prompt adaptation and response post-processing for IDE edit mode.
 */
class IdeEditExecutionAdapterTest {

    private IdeEditExecutionAdapter adapter;
    private PersonaDefinition persona;

    @BeforeEach
    void setUp() {
        adapter = new IdeEditExecutionAdapter();
        persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a test persona.",
                null, List.of(), List.of(), List.of(), null, List.of(), null
        );
    }

    @Test
    @DisplayName("Should support IDE_EDIT mode")
    void shouldSupportIdeEditMode() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        assertThat(adapter.supports(context)).isTrue();
    }

    @Test
    @DisplayName("Should not support CHAT mode")
    void shouldNotSupportChatMode() {
        ExecutionContext context = buildContext(ExecutionMode.CHAT);
        assertThat(adapter.supports(context)).isFalse();
    }

    @Test
    @DisplayName("Should append IDE edit instructions to system prompt")
    void shouldAppendIdeEditInstructions() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String basePrompt = "# Role\nYou are a test persona.";

        String adaptedPrompt = adapter.adaptPrompt(persona, context, basePrompt);

        assertThat(adaptedPrompt).contains("Do not explain");
        assertThat(adaptedPrompt).contains("Do not return JSON");
        assertThat(adaptedPrompt).contains("Do not wrap the answer in Markdown");
        assertThat(adaptedPrompt).contains("Return only the updated code");
        assertThat(adaptedPrompt).contains("Preserve existing behavior");
        assertThat(adaptedPrompt).startsWith(basePrompt);
    }

    @Test
    @DisplayName("Should strip markdown code fences from response")
    void shouldStripMarkdownCodeFences() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "```java\npublic class Foo {\n    // code\n}\n```";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("```");
        assertThat(adapted).contains("public class Foo");
    }

    @Test
    @DisplayName("Should strip markdown code fences without language specifier")
    void shouldStripCodeFencesWithoutLanguage() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "```\npublic class Foo {\n}\n```";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("```");
        assertThat(adapted).contains("public class Foo");
    }

    @Test
    @DisplayName("Should strip leading commentary from response")
    void shouldStripLeadingCommentary() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "Here is the updated code:\n\npublic class Foo {\n    // code\n}";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("Here is the updated code");
        assertThat(adapted).startsWith("public class Foo");
    }

    @Test
    @DisplayName("Should strip trailing commentary from response")
    void shouldStripTrailingCommentary() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "public class Foo {\n    // code\n}\n\nKey changes:\n- Added method";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("Key changes:");
        assertThat(adapted).contains("public class Foo");
    }

    @Test
    @DisplayName("Should handle null response gracefully")
    void shouldHandleNullResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);

        String adapted = adapter.adaptResponse(persona, context, null);

        assertThat(adapted).isNull();
    }

    @Test
    @DisplayName("Should handle blank response gracefully")
    void shouldHandleBlankResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);

        String adapted = adapter.adaptResponse(persona, context, "   ");

        assertThat(adapted).isEqualTo("   ");
    }

    @Test
    @DisplayName("Should preserve clean code response unchanged")
    void shouldPreserveCleanCodeResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "public class Foo {\n    public void bar() {\n        // implementation\n    }\n}";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).isEqualTo(rawResponse);
    }

    @Test
    @DisplayName("Should strip markdown JSON fences from response")
    void shouldStripMarkdownJsonFencesFromResponse() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "```json\n{\"className\": \"Foo\"}\n```";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).doesNotContain("```");
        assertThat(adapted).doesNotStartWith("```json");
    }

    @Test
    @DisplayName("Should not treat Java code with braces as JSON commentary")
    void shouldPreserveJavaCodeWithGenerics() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String rawResponse = "public class Foo {\n    Map<String, Object> data;\n}";

        String adapted = adapter.adaptResponse(persona, context, rawResponse);

        assertThat(adapted).isEqualTo(rawResponse);
    }

    @Test
    @DisplayName("Adapted prompt should not contain JSON format instructions when persona has output_contract")
    void shouldNotContainJsonInstructions() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        String basePrompt = "# Role\nYou are a test persona.\n\n# Output Format\n{json schema here}";

        String adaptedPrompt = adapter.adaptPrompt(persona, context, basePrompt);

        // The adapter appends "Do not return JSON" which overrides any format instructions
        assertThat(adaptedPrompt).contains("Do not return JSON");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ExecutionContext buildContext(ExecutionMode mode) {
        return new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(mode)
                .userInstruction("Add javadoc to this class")
                .build();
    }
}
