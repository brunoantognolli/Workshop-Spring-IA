package com.example.persona.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ModelNameParser}.
 * Verifies correct parsing of model names into persona IDs and execution modes.
 */
class ModelNameParserTest {

    @Test
    @DisplayName("Should parse plain persona name as CHAT mode")
    void shouldParsePlainNameAsChatMode() {
        var result = ModelNameParser.parse("javadoc-persona");

        assertThat(result.personaId()).isEqualTo("javadoc-persona");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.CHAT);
    }

    @Test
    @DisplayName("Should parse ':edit' suffix as IDE_EDIT mode")
    void shouldParseEditSuffix() {
        var result = ModelNameParser.parse("javadoc-persona:edit");

        assertThat(result.personaId()).isEqualTo("javadoc-persona");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.IDE_EDIT);
    }

    @Test
    @DisplayName("Should parse ':apply' suffix as IDE_APPLY mode")
    void shouldParseApplySuffix() {
        var result = ModelNameParser.parse("javadoc-persona:apply");

        assertThat(result.personaId()).isEqualTo("javadoc-persona");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.IDE_APPLY);
    }

    @Test
    @DisplayName("Should parse ':json' suffix as JSON_API mode")
    void shouldParseJsonSuffix() {
        var result = ModelNameParser.parse("adr-reviewer:json");

        assertThat(result.personaId()).isEqualTo("adr-reviewer");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.JSON_API);
    }

    @Test
    @DisplayName("Should parse ':review' suffix as CI_REVIEW mode")
    void shouldParseReviewSuffix() {
        var result = ModelNameParser.parse("adr-reviewer:review");

        assertThat(result.personaId()).isEqualTo("adr-reviewer");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.CI_REVIEW);
    }

    @Test
    @DisplayName("Should parse unit-test-generator:edit correctly")
    void shouldParseUnitTestGeneratorEdit() {
        var result = ModelNameParser.parse("unit-test-generator:edit");

        assertThat(result.personaId()).isEqualTo("unit-test-generator");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.IDE_EDIT);
    }

    @Test
    @DisplayName("Should parse unit-test-generator:apply correctly")
    void shouldParseUnitTestGeneratorApply() {
        var result = ModelNameParser.parse("unit-test-generator:apply");

        assertThat(result.personaId()).isEqualTo("unit-test-generator");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.IDE_APPLY);
    }

    @ParameterizedTest
    @DisplayName("Should parse alternative suffixes correctly")
    @CsvSource({
            "persona:ide-edit,   persona, IDE_EDIT",
            "persona:ide-apply,  persona, IDE_APPLY",
            "persona:json-api,   persona, JSON_API",
            "persona:ci-review,  persona, CI_REVIEW",
            "persona:analysis,   persona, ANALYSIS",
            "persona:chat,       persona, CHAT"
    })
    void shouldParseAlternativeSuffixes(String modelName, String expectedId, ExecutionMode expectedMode) {
        var result = ModelNameParser.parse(modelName.trim());

        assertThat(result.personaId()).isEqualTo(expectedId.trim());
        assertThat(result.executionMode()).isEqualTo(expectedMode);
    }

    @Test
    @DisplayName("Should fallback to CHAT for unknown suffix")
    void shouldFallbackToChatForUnknownSuffix() {
        var result = ModelNameParser.parse("javadoc-persona:unknown");

        assertThat(result.personaId()).isEqualTo("javadoc-persona");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.CHAT);
    }

    @Test
    @DisplayName("Should throw for null model name")
    void shouldThrowForNullModelName() {
        assertThatThrownBy(() -> ModelNameParser.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for blank model name")
    void shouldThrowForBlankModelName() {
        assertThatThrownBy(() -> ModelNameParser.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for model name with only colon prefix")
    void shouldThrowForEmptyPersonaId() {
        assertThatThrownBy(() -> ModelNameParser.parse(":edit"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle trailing colon as CHAT mode")
    void shouldHandleTrailingColonAsChatMode() {
        var result = ModelNameParser.parse("javadoc-persona:");

        assertThat(result.personaId()).isEqualTo("javadoc-persona:");
        assertThat(result.executionMode()).isEqualTo(ExecutionMode.CHAT);
    }
}
