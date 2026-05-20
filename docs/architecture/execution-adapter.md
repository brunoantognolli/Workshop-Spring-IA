# Execution Adapter Architecture

## Problem Statement

The Workshop-Spring-IA project implements AI personas as versionable YAML
definitions. Each persona defines behavior, role, skills, guardrails, and
output contracts. The backend exposes these personas through OpenAI-compatible
endpoints, allowing integration with tools like Continue.dev, Cursor, and
Open WebUI.

**The core problem**: When personas are used inside Continue.dev in Agent/Edit
mode, they respond as consultants — producing text, markdown, or JSON — instead
of producing directly applicable code changes. The IDE's "Apply" button fails
when the persona's output is not formatted as a clean patch or full-file code
block.

**Root cause**: The persona layer conflates *what to do* (semantic behavior)
with *how to format the response* (execution context). A JavaDoc persona
shouldn't care whether it's being called from chat, an IDE edit action,
or a CI pipeline.

## Solution: Execution Adapter Layer

The Execution Adapter layer introduces a clean separation between three
concerns:

### 1. Persona Layer (What)
- Defines behavior, criteria, style, limits, guardrails, and semantic contracts
- Examples: JavaDoc Persona, ADR Reviewer Persona, Unit Test Generator
- **Does not** decide how the response is formatted for the consumer

### 2. Execution Adapter Layer (How)
- Adapts the persona's output for a specific execution context
- Examples: IDE edit, Continue.dev apply, API backend, CI pipeline
- For IDE/Continue, produces directly applicable code changes
- For JSON API, respects the persona's output contract

### 3. Transport/API Layer (Where)
- Exposes OpenAI-compatible endpoints for external clients
- Routes requests to the correct persona + adapter combination
- Handles streaming, SSE, and response formatting

## Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│                 Client Request                   │
│   model: "unit-test-generator:edit"             │
│   messages: [{ role: "user", content: "..." }]  │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│            OpenAiCompatibleController            │
│            (Transport Layer)                     │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│            OpenAiCompatibleService               │
│  ┌──────────────────────────────────────┐       │
│  │  ModelNameParser                      │       │
│  │  "unit-test-generator:edit"           │       │
│  │  → personaId: "unit-test-generator"   │       │
│  │  → mode: IDE_EDIT                     │       │
│  └──────────────────────────────────────┘       │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│           PersonaRuntimeService                  │
│           (Orchestrator)                         │
│                                                  │
│  1. Load PersonaDefinition                       │
│  2. Build ExecutionContext                        │
│  3. Resolve ExecutionAdapter                      │
│  4. adaptPrompt() → add execution instructions   │
│  5. Call LLM via PersonaEngine                    │
│  6. adaptResponse() → strip non-code content     │
│  7. Return adapted response                      │
└──────────────────────┬──────────────────────────┘
                       │
            ┌──────────┴──────────┐
            │                     │
            ▼                     ▼
┌───────────────────┐  ┌───────────────────┐
│   PersonaEngine   │  │ ExecutionAdapter   │
│   (LLM Execution) │  │ Registry          │
│                   │  │                   │
│ • Model selection │  │ • Chat            │
│ • Fallback logic  │  │ • IdeEdit         │
│ • Memory          │  │ • IdeApply        │
│ • Guardrails      │  │ • JsonApi         │
└───────────────────┘  └───────────────────┘
```

## Execution Modes

| Mode       | Output Strategy     | Use Case                                |
|------------|--------------------|-----------------------------------------|
| CHAT       | NATURAL_LANGUAGE   | Normal conversation, markdown allowed   |
| ANALYSIS   | NATURAL_LANGUAGE   | Structured analysis with formatted text |
| IDE_EDIT   | FULL_FILE          | IDE edit actions — code only, no fences  |
| IDE_APPLY  | PATCH_ONLY         | IDE apply — strictest, code/diff only   |
| CI_REVIEW  | NATURAL_LANGUAGE   | CI pipeline review output               |
| JSON_API   | STRUCTURED_JSON    | Respects persona's output_contract      |

## Model Name Convention

Execution modes are specified via model name suffixes:

```
{persona-id}            → CHAT (default)
{persona-id}:edit       → IDE_EDIT
{persona-id}:apply      → IDE_APPLY
{persona-id}:json       → JSON_API
{persona-id}:review     → CI_REVIEW
```

Examples:
- `javadoc-persona` — normal chat mode
- `javadoc-persona:edit` — IDE edit mode (code only)
- `unit-test-generator:apply` — IDE apply mode (strictest)

## How Continue.dev Uses Chat/Edit/Apply

Continue.dev has three interaction modes:

### Chat Mode
- User asks questions, gets natural language + markdown responses
- Model name: `javadoc-persona`
- Adapter: `ChatExecutionAdapter` (passthrough)

### Edit Mode
- User selects code, asks for changes in context
- Continue expects the response to be applicable code
- Model name: `javadoc-persona:edit`
- Adapter: `IdeEditExecutionAdapter` (strips markdown, adds code-only instructions)

### Apply Mode
- Continue applies changes directly to the file
- Response must be valid code or a unified diff
- Model name: `javadoc-persona:apply`
- Adapter: `IdeApplyExecutionAdapter` (strictest — strips everything non-code)

## Adapter Details

### ChatExecutionAdapter
- Passthrough — no modifications to prompt or response
- Used for: CHAT, ANALYSIS, CI_REVIEW modes

### IdeEditExecutionAdapter
- Appends strict instructions: "Do not explain", "Return only code", etc.
- Post-processes response: strips markdown fences, leading/trailing commentary
- Used for: IDE_EDIT mode

### IdeApplyExecutionAdapter
- Strictest mode — strips JSON format instructions from the persona's system prompt
- Appends even stronger instructions than IdeEdit
- Post-processes response: aggressively removes all non-code content
- Used for: IDE_APPLY mode
- **Key behavior**: The persona's JSON output_contract is completely ignored

### JsonApiExecutionAdapter
- Preserves the persona's format instructions (JSON output contract)
- Post-processes response: extracts JSON from markdown fences if present
- Used for: JSON_API mode

## Per-Persona Configuration

Personas can optionally configure execution behavior in their YAML:

```yaml
persona:
  id: javadoc-persona
  # ... other fields ...
  
  execution:
    defaultMode: CHAT
    supportedModes:
      - CHAT
      - JSON_API
      - IDE_EDIT
      - IDE_APPLY
    adapters:
      continue:
        edit:
          outputStrategy: FULL_FILE
          allowMarkdown: false
          allowJson: false
          includeExplanation: false
        apply:
          outputStrategy: UNIFIED_DIFF
          allowMarkdown: false
          allowJson: false
          includeExplanation: false
```

If the `execution:` section is not present, safe defaults are applied
based on the execution mode.

## Limitations

1. **Streaming + post-processing**: In streaming mode, only prompt adaptation
   is applied. Response post-processing (stripping markdown fences, commentary)
   is not applied to individual tokens. This works well for Continue.dev, which
   handles code formatting on its end.

2. **LLM compliance**: The adapter adds instructions to the system prompt, but
   the LLM may not always follow them perfectly. The response post-processing
   acts as a safety net, but it's heuristic-based.

3. **File context extraction**: The adapter does not currently extract
   `activeFileName` or `activeFileContent` from Continue.dev's message format.
   This is a future enhancement.

4. **Apply mode quality**: For production "apply" workflows, using a more
   capable external model (e.g., Claude, GPT-4) is recommended. The local
   persona can handle chat/edit, while a cloud model handles apply.

## Continue.dev Configuration Example

```yaml
models:
  - name: Java Unit Test Agent
    provider: openai
    model: unit-test-generator:edit
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - chat
      - edit

  - name: JavaDoc Persona
    provider: openai
    model: javadoc-persona:edit
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - chat
      - edit

  - name: Java Unit Test Apply
    provider: openai
    model: unit-test-generator:apply
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - apply
```

> **Practical note**: For the `apply` role, consider using a more capable
> external model. Local personas work best for chat and edit roles.
