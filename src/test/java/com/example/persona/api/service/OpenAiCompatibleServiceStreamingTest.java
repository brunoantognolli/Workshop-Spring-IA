package com.example.persona.api.service;

import com.example.persona.execution.ExecutionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleServiceStreamingTest {

    @Test
    @DisplayName("Should default stream to true for IDE_EDIT when stream is null")
    void shouldDefaultStreamForIdeEdit() {
        assertThat(OpenAiCompatibleService.shouldUseStreaming(null, ExecutionMode.IDE_EDIT)).isTrue();
        assertThat(OpenAiCompatibleService.shouldUseStreaming(null, ExecutionMode.IDE_APPLY)).isTrue();
    }

    @Test
    @DisplayName("Should not default stream for CHAT when stream is null")
    void shouldNotDefaultStreamForChat() {
        assertThat(OpenAiCompatibleService.shouldUseStreaming(null, ExecutionMode.CHAT)).isFalse();
    }

    @Test
    @DisplayName("Should respect explicit stream=false")
    void shouldRespectExplicitFalse() {
        assertThat(OpenAiCompatibleService.shouldUseStreaming(false, ExecutionMode.IDE_EDIT)).isFalse();
    }
}
