package com.example.persona.api.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StreamingErrorFormatterTest {

    @Test
    @DisplayName("Should explain SSL certificate failures for cloud fallback")
    void shouldExplainSslFailures() {
        Throwable error = new RuntimeException("Request failed",
                new javax.net.ssl.SSLHandshakeException("PKIX path building failed"));

        String message = StreamingErrorFormatter.toReadableMessage(error);

        assertThat(message).contains("SSL certificate");
        assertThat(message).contains("Ollama");
        assertThat(message).contains("Groq");
    }

    @Test
    @DisplayName("Should explain when Ollama is not running")
    void shouldExplainOllamaConnectionRefused() {
        Throwable error = new RuntimeException("I/O error",
                new java.net.ConnectException("Connection refused: connect"));

        String message = StreamingErrorFormatter.toReadableMessage(
                new RuntimeException("POST http://127.0.0.1:11434/api/chat failed", error));

        assertThat(message).contains("Ollama");
        assertThat(message).contains("11434");
    }

    @Test
    @DisplayName("Should include root cause message when no known pattern matches")
    void shouldIncludeRootCauseMessage() {
        Throwable error = new IllegalStateException("Unexpected persona state");

        String message = StreamingErrorFormatter.toReadableMessage(error);

        assertThat(message).contains("Unexpected persona state");
    }
}
