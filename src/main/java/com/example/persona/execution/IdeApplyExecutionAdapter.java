package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter for {@link ExecutionMode#IDE_APPLY} — the strictest mode, producing
 * only directly applicable code or unified diffs.
 *
 * <p>This adapter is even more aggressive than {@link IdeEditExecutionAdapter}:
 * <ul>
 *   <li>The persona's JSON output_contract is completely ignored</li>
 *   <li>No markdown, no JSON, no explanations are ever allowed</li>
 *   <li>Output is either pure code or a unified diff — nothing else</li>
 * </ul>
 *
 * <p>Designed for Continue.dev's "Apply" action, where the output must be
 * directly patchable into a file without any manual editing.
 */
@Component
public class IdeApplyExecutionAdapter implements ExecutionAdapter {

    private static final Logger log = LoggerFactory.getLogger(IdeApplyExecutionAdapter.class);

    private static final String IDE_APPLY_INSTRUCTIONS = """
            
            # Execution Mode: IDE Apply (Strict)
            You are now operating in strict IDE apply mode. Your output will be applied
            directly to a source file as a code change. Follow these rules WITHOUT EXCEPTION:
            - Return ONLY code or a unified diff. Nothing else.
            - Do not explain.
            - Do not return JSON under any circumstances.
            - Do not use Markdown formatting or code fences.
            - Do not include any text before or after the code.
            - Do not include phrases like "Here is the code" or similar.
            - Preserve all existing code that should not change.
            - Modify only what is necessary to fulfill the request.
            - If returning a full file, include the complete file content from start to end.
            - If returning a patch, use standard unified diff format.
            - Your entire response must be valid, compilable code or a valid unified diff.
            """;

    @Override
    public boolean supports(ExecutionContext context) {
        return context.executionMode() == ExecutionMode.IDE_APPLY;
    }

    @Override
    public String adaptPrompt(PersonaDefinition persona, ExecutionContext context, String systemPrompt) {
        // For IDE_APPLY, we strip any JSON format instructions from the system prompt
        // and replace with strict code-only instructions
        String cleanedPrompt = stripJsonFormatInstructions(systemPrompt);

        StringBuilder sb = new StringBuilder(cleanedPrompt);
        sb.append(IDE_APPLY_INSTRUCTIONS);

        if (context.activeFileName() != null) {
            sb.append("\nTarget file: ").append(context.activeFileName());
        }
        if (context.targetLanguage() != null) {
            sb.append("\nTarget language: ").append(context.targetLanguage());
        }
        if (context.shouldReturnPatch()) {
            sb.append("\nOutput format: unified diff (patch)");
        } else {
            sb.append("\nOutput format: full file content");
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
        result = IdeEditExecutionAdapter.stripCodeFences(result);

        // Aggressively strip all leading commentary
        result = IdeEditExecutionAdapter.stripLeadingCommentary(result);

        // Aggressively strip all trailing commentary
        result = IdeEditExecutionAdapter.stripTrailingCommentary(result);

        log.debug("IDE_APPLY response adapted: {} chars → {} chars", rawResponse.length(), result.length());
        return result;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Removes the "# Output Format" section from the system prompt, which
     * contains JSON schema instructions from the persona's output_contract.
     * In IDE_APPLY mode, these instructions must not reach the LLM.
     */
    private String stripJsonFormatInstructions(String systemPrompt) {
        int outputFormatIndex = systemPrompt.indexOf("# Output Format");
        if (outputFormatIndex > 0) {
            return systemPrompt.substring(0, outputFormatIndex).trim();
        }
        return systemPrompt;
    }
}
