# Persona as Code — Industrial PoC

> **"Persona as Code"** — A versionable, testable unit for AI agents that encapsulates role,
> skills, behavioral boundaries, guardrails, and validation tests in a single YAML file.

[![Java 21](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring AI 1.1.4](https://img.shields.io/badge/Spring%20AI-1.1.4-orange)](https://spring.io/projects/spring-ai)

---

## What is Persona as Code?

A **Persona** is an AI agent identity defined entirely in YAML:

```yaml
persona:
  id: javadoc-persona
  version: 1.0.0
  role: "You are a senior Java documentation engineer..."
  model:
    primary:   { provider: ollama, model: mistral:7b }
    fallback:  { provider: openai, model: gpt-4o-mini, trigger: CONTRACT_FAILURE }
  skills:
    - { id: java_doc_style_guide, path: "classpath:/skills/java-doc-style-guide.md" }
  boundaries:
    - "Do not invent method signatures not present in the source"
  guardrails:
    - { type: content, rule: "Response must not contain 'TODO' or 'FIXME'" }
  output_contract:
    format: json
    schema: "com.example.persona.examples.contracts.JavaDocOutput"
  tests:
    - { type: fact_coverage, threshold: 0.9 }
    - { type: metamorphic, threshold: 0.85 }
```

**To add a new persona:** create a YAML file + a Java Record. Zero engine changes.

---

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                      PersonaEngine                             │
│                                                                │
│  PersonaLoader ──► PersonaDefinition (YAML)                   │
│        │                                                       │
│        ▼                                                       │
│  SkillRegistrar ──► .md files ──► FunctionCallback (tools)    │
│        │                                                       │
│        ▼                                                       │
│  BoundaryAdvisor (CallAdvisor) ──► guardrail enforcement      │
│        │                                                       │
│        ▼                                                       │
│  PRIMARY: Ollama mistral:7b (local, free)                     │
│        │                                                       │
│        ▼ (contract failure / on_error)                        │
│  FALLBACK: Cloud model (OpenAI / Anthropic / Gemini)          │
│        │                                                       │
│        ▼                                                       │
│  OutputValidator ──► JSON Schema (from Java Record)           │
└────────────────────────────────────────────────────────────────┘
```

---

## Included Personas

| Persona ID | Role | Output Contract |
|---|---|---|
| `javadoc-persona` | Senior Java documentation engineer | `JavaDocOutput` |
| `adr-reviewer` | Expert software architect (ADR review) | `AdrReviewOutput` |
| `unit-test-generator` | Expert Java test engineer (JUnit 5 + Mockito) | `UnitTestGeneratorOutput` |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose
- (Optional) OpenAI API key for cloud fallback

### 1. Start Ollama with mistral:7b

```bash
docker-compose up -d
docker-compose exec ollama ollama pull mistral
```

### 2. Run the application

```bash
mvn spring-boot:run
```

### 3. Test the REST API

```bash
# List all personas
curl http://localhost:8080/api/personas

# Run the JavaDoc persona
curl -X POST http://localhost:8080/api/personas/javadoc-persona/run \
  -H "Content-Type: application/json" \
  -d '{"input": "public class Foo { public void bar() { System.out.println(\"hello\"); } }"}'

# Run the ADR Reviewer persona
curl -X POST http://localhost:8080/api/personas/adr-reviewer/run \
  -H "Content-Type: application/json" \
  -d '{"input": "# ADR-001: Use PostgreSQL\n\n## Context\nWe need a database.\n\n## Decision\nUse PostgreSQL.\n\n## Consequences\n### Negative\n- Operational overhead"}'
```

### 4. Use as an OpenAI-compatible model (Cursor, Continue.dev)

```bash
# List available "models" (personas)
curl http://localhost:8080/v1/models

# Chat completion (OpenAI format)
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "javadoc-persona",
    "messages": [{"role": "user", "content": "Document: public class Foo { public void bar() {} }"}]
  }'
```

#### Execution Modes (Edit / Apply)

Personas support execution mode suffixes that control output formatting:

| Model Name | Mode | Output |
|---|---|---|
| `javadoc-persona` | Chat | Natural language / markdown |
| `javadoc-persona:edit` | IDE Edit | Code only, no explanations |
| `javadoc-persona:apply` | IDE Apply | Strictest — raw code or unified diff |
| `javadoc-persona:json` | JSON API | Respects output_contract schema |

```bash
# IDE Edit mode — returns code only
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "unit-test-generator:edit",
    "messages": [{"role": "user", "content": "Generate tests for: public class Foo { public int add(int a, int b) { return a + b; } }"}]
  }'
```

See [docs/architecture/execution-adapter.md](docs/architecture/execution-adapter.md) for full details.

#### Cursor IDE Setup
1. Settings → Features → Beta → Enable OpenAI API
2. Set API Key: `persona-engine-local` (any string)
3. Set Base URL: `http://localhost:8080/v1`
4. Select model: `javadoc-persona` or `adr-reviewer`

#### Continue.dev Setup (VS Code)

```yaml
# ~/.continue/config.yaml
models:
  # Chat mode — natural language responses
  - name: JavaDoc Persona
    provider: openai
    model: javadoc-persona
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - chat

  # Edit mode — code-only responses for IDE edit actions
  - name: Java Unit Test Agent
    provider: openai
    model: unit-test-generator:edit
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - chat
      - edit

  # Apply mode — strictest, for direct code application
  - name: Java Unit Test Apply
    provider: openai
    model: unit-test-generator:apply
    apiBase: http://localhost:8080/v1
    apiKey: local
    roles:
      - apply
```

> **Tip:** For the `apply` role, consider using a more capable external model.
> Local personas work best for `chat` and `edit` roles.

---

## Running Tests

```bash
# Unit tests only (no Ollama required)
mvn test -Dgroups="!integration"

# Integration tests (requires Ollama running with mistral)
mvn test -Dgroups="integration"

# All tests
mvn test
```

---

## Using the Cloud Fallback

Set environment variables before starting the application:

```bash
# OpenAI (default fallback)
export OPENAI_API_KEY=sk-...
export FALLBACK_PROVIDER=openai
export FALLBACK_MODEL=gpt-4o-mini

# Anthropic (Claude)
# 1. Add dependency: spring-ai-anthropic-spring-boot-starter
# 2. Set:
export ANTHROPIC_API_KEY=sk-ant-...
export FALLBACK_PROVIDER=anthropic
export FALLBACK_MODEL=claude-3-haiku-20240307
```

The fallback is triggered automatically when:
- The local Ollama model's output **fails the output contract validation** (default)
- Or when Ollama throws an error (set `FALLBACK_TRIGGER=ON_ERROR`)

---

## Adding a New Persona

1. **Create the output contract** (Java Record):
```java
// src/main/java/com/example/persona/examples/contracts/MyPersonaOutput.java
public record MyPersonaOutput(String result, List<String> insights) {}
```

2. **Create the persona YAML**:
```yaml
# src/main/resources/personas/my-persona-1.0.0.yaml
persona:
  id: my-persona
  version: 1.0.0
  role: "You are a ..."
  skills: []
  boundaries:
    - "Do not ..."
  guardrails:
    - type: content
      rule: "Response must not contain 'bad-pattern'"
  output_contract:
    format: json
    schema: "com.example.persona.examples.contracts.MyPersonaOutput"
  tests:
    - type: fact_coverage
      threshold: 0.85
```

3. **Use it immediately** (no engine changes):
```bash
curl -X POST http://localhost:8080/api/personas/my-persona/run \
  -H "Content-Type: application/json" \
  -d '{"input": "your input here"}'
```

---

## Project Structure

```
src/main/java/com/example/persona/
├── core/          PersonaLoader, PersonaEngine, PersonaDefinition
├── execution/     ExecutionAdapter, ExecutionContext, ModelNameParser,
│                  PersonaRuntimeService, ChatAdapter, IdeEditAdapter,
│                  IdeApplyAdapter, JsonApiAdapter, AdapterRegistry
├── skills/        SkillRegistrar (Markdown → FunctionCallback)
├── guardrails/    BoundaryAdvisor (CallAdvisor), BoundaryViolationException
├── contracts/     OutputValidator, ValidationResult
├── api/           PersonaController, OpenAiCompatibleController, DTOs
└── examples/      JavaDocPersonaRunner, AdrReviewerPersonaRunner, contracts/

src/main/resources/
├── personas/      javadoc-persona-1.0.0.yaml, adr-reviewer-1.0.0.yaml,
│                  unit-test-generator-1.0.0.yaml
└── skills/        java-doc-style-guide.md, java-complexity-analysis.md,
                   adr-format-rules.md, architecture-patterns.md

src/test/java/com/example/persona/
├── core/          PersonaLoaderTest (unit)
├── execution/     ModelNameParserTest, ExecutionAdapterRegistryTest,
│                  IdeEditExecutionAdapterTest, IdeApplyExecutionAdapterTest,
│                  ExecutionContextBuilderTest
├── guardrails/    BoundaryAdvisorTest (unit)
├── contracts/     OutputValidatorTest (unit)
└── integration/   JavaDocFactCoverageTest, JavaDocMetamorphicTest,
                   AdrReviewerFactCoverageTest
```

---

## Key Concepts

| Concept | Implementation |
|---|---|
| **Persona** | YAML file → `PersonaDefinition` (Java Record) |
| **Skill** | Markdown file → `FunctionCallback` (LLM tool) |
| **Guardrail** | `BoundaryAdvisor` (`CallAdvisor`) pattern matching |
| **Output Contract** | `BeanOutputConverter<T>` + JSON Schema validation |
| **Execution Adapter** | Adapts persona output for IDE/API/CI contexts |
| **Execution Mode** | `:edit`, `:apply`, `:json` suffixes on model name |
| **Fact Coverage Test** | `FactCheckingEvaluator` (LLM-as-Judge) |
| **Metamorphic Test** | Variable renaming + Jaccard similarity |
| **Local-first** | Ollama `mistral:7b`, no cloud cost by default |
| **Fallback** | Cloud model via env vars, triggered on contract failure |
| **OpenAI-compat API** | `/v1/models` + `/v1/chat/completions` |
