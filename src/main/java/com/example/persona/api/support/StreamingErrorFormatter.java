package com.example.persona.api.support;

import com.example.persona.core.PersonaEngine;

import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;

/**
 * Turns low-level LLM/HTTP exceptions into short, actionable messages for API clients.
 */
public final class StreamingErrorFormatter {

    private StreamingErrorFormatter() {
    }

    /**
     * Builds a user-facing error message from the exception cause chain.
     */
    public static String toReadableMessage(Throwable error) {
        if (error == null) {
            return "Unknown error while running the persona.";
        }

        if (error instanceof PersonaEngine.PersonaExecutionException) {
            return error.getMessage();
        }

        if (containsCause(error, "SunCertPathBuilderException", "PKIX path building failed",
                "certificate_unknown", "unable to find valid certification path",
                "CRYPT_E_NO_REVOCATION_CHECK", "revocation")) {
            return """
                    Cloud fallback (Groq/OpenAI-compatible API) failed: SSL certificate could not be verified.
                    This is common on Windows behind proxy, antivirus, or when certificate revocation checks fail.
                    Try:
                    1) Start Ollama locally (ollama serve) so the primary model is used and fallback is avoided.
                    2) Fix HTTPS to api.groq.com from this machine (curl/PowerShell test).
                    3) Import your corporate CA into the Java truststore if you use a TLS-inspecting proxy.
                    """.trim().replace("\n", " ");
        }

        if (containsCause(error, "Connection refused") && containsCause(error, "11434")) {
            return """
                    Local model (Ollama) is not reachable at http://127.0.0.1:11434.
                    Start it with: ollama serve
                    Then pull a model, e.g.: ollama pull mistral
                    """.trim().replace("\n", " ");
        }

        if (containsCause(error, ConnectException.class.getName(), "Connection refused")) {
            return "Could not connect to the configured LLM endpoint. "
                    + "Check that Ollama is running or that the cloud API key and network are valid.";
        }

        if (containsCause(error, "401", "Unauthorized", "invalid_api_key")) {
            return "Cloud fallback authentication failed. Check GROQ_API_KEY (or OPENAI_API_KEY) in the server environment.";
        }

        Throwable root = rootCause(error);
        String rootMessage = root.getMessage();
        if (rootMessage != null && !rootMessage.isBlank()) {
            return "Persona execution failed: " + rootMessage;
        }

        return "Persona execution failed: " + error.getClass().getSimpleName();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean containsCause(Throwable error, String... needles) {
        Set<String> seen = new HashSet<>();
        Throwable current = error;
        while (current != null && seen.add(current.getClass().getName() + "@" + System.identityHashCode(current))) {
            String text = current.getClass().getName() + " " + String.valueOf(current.getMessage());
            for (String needle : needles) {
                if (text.contains(needle)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
