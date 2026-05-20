package com.example.persona.api.support;

import com.example.persona.api.dto.ChatCompletionRequest;

import java.util.List;

/**
 * Extracts user-facing input from OpenAI-compatible chat requests.
 *
 * <p>Continue.dev Edit often sends a {@code user} prompt plus an {@code assistant}
 * prefill (partial answer). We must preserve both so the model continues in the
 * expected format instead of restarting from scratch.
 */
public final class OpenAiMessageExtractor {

    private OpenAiMessageExtractor() {
    }

    /**
     * Builds the text passed to {@link com.example.persona.core.PersonaEngine} as user input.
     */
    public static String extractUserInput(ChatCompletionRequest request) {
        List<ChatCompletionRequest.Message> messages = request.messages();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("No messages found in request");
        }

        ChatCompletionRequest.Message last = messages.get(messages.size() - 1);
        if ("assistant".equalsIgnoreCase(last.role())) {
            String userPart = lastUserMessage(messages);
            String prefill = last.content() != null ? last.content() : "";
            if (userPart.isBlank()) {
                throw new IllegalArgumentException("No user message found before assistant prefill");
            }
            return userPart + "\n\nContinue your response exactly from this point "
                    + "(do not repeat prior text, no explanations, code only):\n"
                    + prefill;
        }

        String userInput = lastUserMessage(messages);
        if (userInput.isBlank()) {
            throw new IllegalArgumentException("No user message found in request");
        }
        return userInput;
    }

    /** True when the request matches Continue.dev inline edit / apply prompts. */
    public static boolean looksLikeContinueEditRequest(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }
        String lower = userInput.toLowerCase();
        return lower.contains("code to rewrite")
                || lower.contains("here is the rewritten code")
                || lower.contains("fill in the \"[blank]\"")
                || lower.contains("suggested edit")
                || lower.contains("original code:");
    }

    private static String lastUserMessage(List<ChatCompletionRequest.Message> messages) {
        return messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.role()))
                .reduce((first, second) -> second)
                .map(ChatCompletionRequest.Message::content)
                .orElse("");
    }
}
