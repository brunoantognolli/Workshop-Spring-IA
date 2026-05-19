package com.example.persona.guardrails;

import com.example.persona.core.PersonaDefinition.GuardrailDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring AI {@link CallAdvisor} that enforces a persona's behavioral boundaries
 * and guardrail rules on every LLM response.
 *
 * <h3>Guardrail detection strategy:</h3>
 * <p>Scans guardrail rules for single-quoted forbidden patterns (e.g., {@code 'TODO'}, {@code 'FIXME'}).
 * If any forbidden pattern is found in the LLM's response, a
 * {@link BoundaryViolationException} is thrown — preventing the response from
 * reaching the caller.
 *
 * <h3>Instantiation:</h3>
 * <p>Created per-persona by {@code PersonaEngine}. Not a Spring bean — instantiated
 * with the persona's specific boundaries and guardrails.
 */
public class BoundaryAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(BoundaryAdvisor.class);
    private static final Pattern QUOTED_PATTERN = Pattern.compile("'([^']+)'");

    private final List<String> boundaries;
    private final List<String> forbiddenPatterns;

    /**
     * Creates a BoundaryAdvisor for the given persona configuration.
     *
     * @param boundaries  natural-language boundary rules (injected into system prompt context)
     * @param guardrails  typed guardrail rules (used for response pattern matching)
     */
    public BoundaryAdvisor(List<String> boundaries, List<GuardrailDef> guardrails) {
        this.boundaries = boundaries != null ? boundaries : List.of();
        this.forbiddenPatterns = extractForbiddenPatterns(guardrails);
        log.debug("BoundaryAdvisor initialized with {} boundaries, {} forbidden patterns",
                this.boundaries.size(), this.forbiddenPatterns.size());
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // ── PRE-PROCESSING: pass through (system prompt already contains boundaries) ──
        ChatClientResponse response = chain.nextCall(request);

        // ── POST-PROCESSING: scan response for guardrail violations ──
        String content = extractContent(response);
        if (content != null) {
            checkForViolations(content);
        }

        return response;
    }

    @Override
    public String getName() {
        return "BoundaryAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Checks the response content against all forbidden patterns.
     * Throws {@link BoundaryViolationException} on the first match.
     */
    private void checkForViolations(String content) {
        String contentUpper = content.toUpperCase();
        for (String pattern : forbiddenPatterns) {
            if (contentUpper.contains(pattern.toUpperCase())) {
                log.warn("Guardrail violation detected: pattern '{}' found in response", pattern);
                throw new BoundaryViolationException(pattern, content);
            }
        }
    }

    /**
     * Extracts single-quoted strings from guardrail rule texts.
     * Example: "Response must not contain 'TODO' or 'FIXME'" → ["TODO", "FIXME"]
     */
    private List<String> extractForbiddenPatterns(List<GuardrailDef> guardrails) {
        if (guardrails == null) return List.of();

        List<String> patterns = new ArrayList<>();
        for (GuardrailDef guardrail : guardrails) {
            Matcher matcher = QUOTED_PATTERN.matcher(guardrail.rule());
            while (matcher.find()) {
                patterns.add(matcher.group(1));
            }
        }
        return patterns;
    }

    /**
     * Safely extracts the text content from a ChatClientResponse.
     */
    private String extractContent(ChatClientResponse response) {
        try {
            return response.chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            log.debug("Could not extract content for boundary checking: {}", e.getMessage());
            return null;
        }
    }
}
