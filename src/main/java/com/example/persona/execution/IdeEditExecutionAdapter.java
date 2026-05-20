package com.example.persona.execution;

import com.example.persona.api.support.OpenAiMessageExtractor;
import com.example.persona.core.PersonaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for {@link ExecutionMode#IDE_EDIT} — produces code suitable for
 * IDE edit actions (e.g., Continue.dev edit mode).
 *
 * <h3>Prompt adaptation:</h3>
 * <p>Appends strict instructions telling the LLM to return only code, without
 * explanations, JSON wrappers, or markdown formatting.
 *
 * <h3>Response adaptation:</h3>
 * <p>Strips markdown code fences and any leading/trailing commentary that
 * the LLM may have included despite the prompt instructions.
 */
@Component
public class IdeEditExecutionAdapter implements ExecutionAdapter {

    private static final Logger log = LoggerFactory.getLogger(IdeEditExecutionAdapter.class);

    /** Matches markdown code fences like ```java ... ``` or ``` ... ``` */
    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("^```[a-zA-Z]*\\s*\\n(.*?)\\n```\\s*$", Pattern.DOTALL);

    private static final String IDE_EDIT_INSTRUCTIONS = """
            
            # Execution Mode: IDE Edit
            You are now operating in IDE edit mode. Follow these rules STRICTLY:
            - Do not explain your changes.
            - Do not return JSON.
            - Do not wrap the answer in Markdown code fences.
            - Return only the updated code.
            - Preserve existing behavior — modify only what is necessary.
            - Do not include commentary before or after the code.
            - If the full file is requested, return the full updated file content.
            - If a patch is requested, return a unified diff.
            - Do not start with phrases like "Here is the updated code" or similar.
            """;

    private static final String CONTINUE_EDIT_INSTRUCTIONS = """
            
            # Continue.dev Inline Edit
            The user message includes IDE context (code to rewrite, prefix/suffix, or assistant prefill).
            - Output ONLY the rewritten code that should replace the highlighted section.
            - Do NOT repeat prefix or suffix from the prompt.
            - Do NOT output markdown code fences (no ``` lines).
            - Do NOT output natural language before or after the code.
            - Match the indentation of the original highlighted lines.
            """;

    @Override
    public boolean supports(ExecutionContext context) {
        return context.executionMode() == ExecutionMode.IDE_EDIT;
    }

    @Override
    public String adaptPrompt(PersonaDefinition persona, ExecutionContext context, String systemPrompt) {
        StringBuilder sb = new StringBuilder(systemPrompt);

        // Override any JSON format instructions from the persona's output_contract
        sb.append(IDE_EDIT_INSTRUCTIONS);

        if (context.userInstruction() != null
                && OpenAiMessageExtractor.looksLikeContinueEditRequest(context.userInstruction())) {
            sb.append(CONTINUE_EDIT_INSTRUCTIONS);
        }

        // Add file context if available
        if (context.activeFileName() != null) {
            sb.append("\nTarget file: ").append(context.activeFileName());
        }
        if (context.targetLanguage() != null) {
            sb.append("\nTarget language: ").append(context.targetLanguage());
        }

        return sb.toString();
    }

    @Override
    public String adaptResponse(PersonaDefinition persona, ExecutionContext context, String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }

        String result = rawResponse.trim();

        // Strip markdown code fences
        result = stripCodeFences(result);

        // Strip leading commentary (lines before actual code)
        result = stripLeadingCommentary(result);

        // Strip trailing commentary (lines after code block ends)
        result = stripTrailingCommentary(result);

        log.debug("IDE_EDIT response adapted: {} chars → {} chars", rawResponse.length(), result.length());
        return result;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Strips markdown code fences (e.g., ```java ... ```) from the response.
     */
    static String stripCodeFences(String text) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(text.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }

        // Handle multiple code fence blocks — take the first one
        if (text.contains("```")) {
            int start = text.indexOf("```");
            int newlineAfterStart = text.indexOf('\n', start);
            int end = text.indexOf("```", newlineAfterStart + 1);
            if (newlineAfterStart > 0 && end > newlineAfterStart) {
                return text.substring(newlineAfterStart + 1, end).trim();
            }
        }

        return text;
    }

    /**
     * Strips leading commentary lines (non-code text before the actual code).
     * Detects common patterns like "Here is the updated code:" and similar.
     */
    static String stripLeadingCommentary(String text) {
        String[] lines = text.split("\n");
        int codeStartIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // Skip empty lines at the beginning
            if (line.isEmpty()) {
                codeStartIndex = i + 1;
                continue;
            }
            // Detect commentary patterns
            if (isCommentaryLine(line)) {
                codeStartIndex = i + 1;
                continue;
            }
            break;
        }

        if (codeStartIndex > 0 && codeStartIndex < lines.length) {
            return String.join("\n",
                    java.util.Arrays.copyOfRange(lines, codeStartIndex, lines.length));
        }
        return text;
    }

    /**
     * Strips trailing commentary lines after the code ends.
     * Also removes empty lines that separate code from trailing commentary.
     */
    static String stripTrailingCommentary(String text) {
        String[] lines = text.split("\n");

        // Scan backwards: find the last line of actual code.
        // Skip trailing commentary and empty lines that are part of the trailing block.
        int lastCodeLine = lines.length - 1;

        // Phase 1: skip trailing empty lines
        while (lastCodeLine >= 0 && lines[lastCodeLine].trim().isEmpty()) {
            lastCodeLine--;
        }

        // Phase 2: skip commentary + empty line sequences from the end
        boolean changed = true;
        while (changed) {
            changed = false;
            // Skip commentary lines
            while (lastCodeLine >= 0 && isCommentaryLine(lines[lastCodeLine].trim())) {
                lastCodeLine--;
                changed = true;
            }
            // Skip empty lines between code and commentary
            while (lastCodeLine >= 0 && lines[lastCodeLine].trim().isEmpty()) {
                lastCodeLine--;
                changed = true;
            }
        }

        if (lastCodeLine < lines.length - 1 && lastCodeLine >= 0) {
            return String.join("\n",
                    java.util.Arrays.copyOfRange(lines, 0, lastCodeLine + 1));
        }
        return text;
    }

    /**
     * Checks if a line looks like natural-language commentary rather than code.
     * Detects common LLM commentary patterns including markdown lists.
     */
    private static boolean isCommentaryLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("here is")
                || lower.startsWith("here's")
                || lower.startsWith("below is")
                || lower.startsWith("the following")
                || lower.startsWith("i have")
                || lower.startsWith("i've")
                || lower.startsWith("this is")
                || lower.startsWith("note:")
                || lower.startsWith("explanation:")
                || lower.startsWith("changes made:")
                || lower.startsWith("summary:")
                || lower.startsWith("key changes:")
                || lower.startsWith("changes:")
                || (lower.startsWith("**") && lower.endsWith("**"))
                || lower.matches("^-\\s+.*")          // markdown unordered list item
                || lower.matches("^\\d+\\.\\s+.*");   // markdown ordered list item
    }
}
