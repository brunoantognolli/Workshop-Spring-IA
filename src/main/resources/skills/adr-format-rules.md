# ADR Format Rules

This skill provides the official format and quality criteria for Architecture Decision Records (ADRs),
following the Nygard template and modern architectural governance practices.

## Standard ADR Structure

A well-formed ADR must contain all of the following sections:

```markdown
# ADR-{NUMBER}: {Short Title}

**Status**: Proposed | Accepted | Deprecated | Superseded
**Date**: YYYY-MM-DD
**Deciders**: {list of stakeholders}

## Context
{Describe the forces at play: technical, business, social. Non-neutral.}

## Decision
{State the decision in active voice: "We will use X because Y."}

## Consequences
### Positive
- {benefit 1}
### Negative
- {drawback 1 — REQUIRED}
### Risks
- {risk 1}
```

## Quality Criteria

An ADR is **APPROVE-able** when it:
- Has a clear, specific decision statement (not vague)
- Lists at least ONE explicit negative consequence or trade-off
- Provides context that justifies the decision
- Is scoped to a single architectural concern

An ADR **NEEDS_REVISION** when:
- Trade-offs are missing or too vague
- The decision statement is ambiguous
- Context doesn't justify the decision made
- Multiple unrelated decisions are bundled together

An ADR should be **REJECTED** when:
- The decision contradicts established architecture principles without justification
- The proposed approach has known critical flaws not acknowledged
- The ADR reverses a previous decision without superseding it properly

## Trade-off Analysis Framework

For every ADR, evaluate these dimensions:
- **Performance vs. Maintainability**: Does speed come at a readability cost?
- **Simplicity vs. Flexibility**: Is this over-engineered for current needs?
- **Consistency vs. Pragmatism**: Does this align with existing patterns?
- **Short-term vs. Long-term**: What is the technical debt implications?

## Common ADR Anti-patterns

- **No Trade-offs**: Presenting only benefits without acknowledging drawbacks
- **Vague Decision**: "We will use a better approach" (not specific enough)
- **Missing Context**: Decision without explaining WHY this was needed now
- **Too Broad**: One ADR covering multiple unrelated architectural concerns
