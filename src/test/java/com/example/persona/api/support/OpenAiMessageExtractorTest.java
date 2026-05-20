package com.example.persona.api.support;

import com.example.persona.api.dto.ChatCompletionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiMessageExtractorTest {

    @Test
    @DisplayName("Should use last user message when no assistant prefill")
    void shouldUseLastUserMessage() {
        var request = new ChatCompletionRequest(
                "unit-test-generator:edit",
                List.of(
                        new ChatCompletionRequest.Message("user", "first"),
                        new ChatCompletionRequest.Message("user", "second")
                ),
                null, null, true);

        assertThat(OpenAiMessageExtractor.extractUserInput(request)).isEqualTo("second");
    }

    @Test
    @DisplayName("Should append assistant prefill for Continue edit continuation")
    void shouldAppendAssistantPrefill() {
        var request = new ChatCompletionRequest(
                "unit-test-generator:edit",
                List.of(
                        new ChatCompletionRequest.Message("user", "Rewrite this code:\n```java\nclass A {}\n```"),
                        new ChatCompletionRequest.Message("assistant", "```java\n")
                ),
                null, null, true);

        String extracted = OpenAiMessageExtractor.extractUserInput(request);

        assertThat(extracted).contains("Rewrite this code");
        assertThat(extracted).contains("Continue your response exactly from this point");
        assertThat(extracted).endsWith("```java\n");
    }

    @Test
    @DisplayName("Should detect Continue edit prompt patterns")
    void shouldDetectContinueEditPrompt() {
        assertThat(OpenAiMessageExtractor.looksLikeContinueEditRequest(
                "Here is the rewritten code:\n```java\nclass A {}\n```")).isTrue();
        assertThat(OpenAiMessageExtractor.looksLikeContinueEditRequest("just chat")).isFalse();
    }

    @Test
    @DisplayName("Should fail when messages are empty")
    void shouldFailWhenEmpty() {
        var request = new ChatCompletionRequest("m", List.of(), null, null, false);
        assertThatThrownBy(() -> OpenAiMessageExtractor.extractUserInput(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
