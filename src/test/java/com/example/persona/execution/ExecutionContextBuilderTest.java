package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ExecutionContextBuilder}.
 * Verifies default resolution and persona config override behavior.
 */
class ExecutionContextBuilderTest {

    @Test
    @DisplayName("Should use CHAT defaults when no config is provided")
    void shouldUseChatDefaults() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.CHAT)
                .userInstruction("hello")
                .build();

        assertThat(context.executionMode()).isEqualTo(ExecutionMode.CHAT);
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.NATURAL_LANGUAGE);
        assertThat(context.shouldReturnMarkdown()).isTrue();
        assertThat(context.shouldReturnJson()).isFalse();
        assertThat(context.shouldReturnFullFile()).isFalse();
        assertThat(context.shouldReturnPatch()).isFalse();
    }

    @Test
    @DisplayName("Should use IDE_EDIT defaults when no config is provided")
    void shouldUseIdeEditDefaults() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.IDE_EDIT)
                .userInstruction("edit this")
                .build();

        assertThat(context.executionMode()).isEqualTo(ExecutionMode.IDE_EDIT);
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.FULL_FILE);
        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.shouldReturnJson()).isFalse();
        assertThat(context.shouldReturnFullFile()).isTrue();
        assertThat(context.shouldReturnPatch()).isFalse();
    }

    @Test
    @DisplayName("Should use IDE_APPLY defaults when no config is provided")
    void shouldUseIdeApplyDefaults() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.IDE_APPLY)
                .userInstruction("apply this")
                .build();

        assertThat(context.executionMode()).isEqualTo(ExecutionMode.IDE_APPLY);
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.PATCH_ONLY);
        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.shouldReturnJson()).isFalse();
        assertThat(context.shouldReturnFullFile()).isFalse();
        assertThat(context.shouldReturnPatch()).isTrue();
    }

    @Test
    @DisplayName("Should use JSON_API defaults when no config is provided")
    void shouldUseJsonApiDefaults() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.JSON_API)
                .userInstruction("analyze this")
                .build();

        assertThat(context.executionMode()).isEqualTo(ExecutionMode.JSON_API);
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.STRUCTURED_JSON);
        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.shouldReturnJson()).isTrue();
        assertThat(context.shouldReturnFullFile()).isFalse();
        assertThat(context.shouldReturnPatch()).isFalse();
    }

    @Test
    @DisplayName("Should apply persona execution config overrides")
    void shouldApplyPersonaConfigOverrides() {
        PersonaDefinition.AdapterConfig adapterConfig = new PersonaDefinition.AdapterConfig(
                "UNIFIED_DIFF", false, false, false
        );

        PersonaDefinition.ExecutionConfig executionConfig = new PersonaDefinition.ExecutionConfig(
                "CHAT",
                List.of("CHAT", "IDE_EDIT"),
                Map.of("continue", Map.of("edit", adapterConfig))
        );

        PersonaDefinition persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a test persona.",
                null, List.of(), List.of(), List.of(), null, List.of(),
                executionConfig
        );

        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.IDE_EDIT)
                .userInstruction("edit this")
                .applyPersonaConfig(persona)
                .build();

        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.UNIFIED_DIFF);
        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.shouldReturnJson()).isFalse();
    }

    @Test
    @DisplayName("Should use defaults when persona has no execution config")
    void shouldUseDefaultsWhenNoExecutionConfig() {
        PersonaDefinition persona = new PersonaDefinition(
                "test-persona", "1.0.0", "You are a test persona.",
                null, List.of(), List.of(), List.of(), null, List.of(), null
        );

        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.IDE_EDIT)
                .userInstruction("edit this")
                .applyPersonaConfig(persona)
                .build();

        // Should still get IDE_EDIT defaults
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.FULL_FILE);
        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.shouldReturnJson()).isFalse();
    }

    @Test
    @DisplayName("Should default clientType to UNKNOWN")
    void shouldDefaultClientTypeToUnknown() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.CHAT)
                .userInstruction("hello")
                .build();

        assertThat(context.clientType()).isEqualTo(ClientType.UNKNOWN);
    }

    @Test
    @DisplayName("Should preserve explicitly set values over defaults")
    void shouldPreserveExplicitValues() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.CHAT)
                .userInstruction("hello")
                .shouldReturnMarkdown(false) // override CHAT default
                .outputStrategy(OutputStrategy.CODE_BLOCK_ONLY) // override CHAT default
                .build();

        assertThat(context.shouldReturnMarkdown()).isFalse();
        assertThat(context.outputStrategy()).isEqualTo(OutputStrategy.CODE_BLOCK_ONLY);
    }

    @Test
    @DisplayName("Should set nullable fields correctly")
    void shouldSetNullableFields() {
        ExecutionContext context = new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(ExecutionMode.IDE_EDIT)
                .userInstruction("edit")
                .activeFileName("Foo.java")
                .activeFileContent("public class Foo {}")
                .targetLanguage("JAVA")
                .clientType(ClientType.CONTINUE_DEV)
                .build();

        assertThat(context.activeFileName()).isEqualTo("Foo.java");
        assertThat(context.activeFileContent()).isEqualTo("public class Foo {}");
        assertThat(context.targetLanguage()).isEqualTo("JAVA");
        assertThat(context.clientType()).isEqualTo(ClientType.CONTINUE_DEV);
    }
}
