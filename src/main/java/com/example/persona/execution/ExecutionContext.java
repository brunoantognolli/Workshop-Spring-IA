package com.example.persona.execution;

/**
 * Immutable context object carrying all metadata about how a persona
 * execution should be adapted for a specific environment.
 *
 * <p>Built by {@link ExecutionContextBuilder} from a combination of:
 * <ul>
 *   <li>Parsed model name (e.g., {@code javadoc-persona:edit})</li>
 *   <li>Persona YAML configuration ({@code execution:} section)</li>
 *   <li>Request metadata (headers, messages, etc.)</li>
 * </ul>
 *
 * @param personaId         the persona ID without execution suffix
 * @param personaVersion    the persona version from the YAML definition
 * @param executionMode     how the output should be formatted
 * @param clientType        the calling client (Continue.dev, Cursor, API, etc.)
 * @param targetLanguage    the programming language of the target file (nullable)
 * @param activeFileName    the file being edited in the IDE (nullable)
 * @param activeFileContent the current content of the active file (nullable)
 * @param userInstruction   the user's original instruction/prompt
 * @param outputStrategy    the specific output format to produce
 * @param shouldReturnMarkdown  whether markdown formatting is allowed
 * @param shouldReturnJson      whether JSON output is allowed
 * @param shouldReturnFullFile  whether the full file content should be returned
 * @param shouldReturnPatch     whether a unified diff/patch should be returned
 */
public record ExecutionContext(
        String personaId,
        String personaVersion,
        ExecutionMode executionMode,
        ClientType clientType,
        String targetLanguage,
        String activeFileName,
        String activeFileContent,
        String userInstruction,
        OutputStrategy outputStrategy,
        boolean shouldReturnMarkdown,
        boolean shouldReturnJson,
        boolean shouldReturnFullFile,
        boolean shouldReturnPatch
) {}
