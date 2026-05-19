# Java Complexity Analysis

This skill provides structured guidelines for analyzing and reporting the complexity
of Java classes and methods.

## Complexity Metrics

### Cyclomatic Complexity
Count the number of independent paths through the code:
- **1-4**: Low complexity — well-structured, easy to test
- **5-10**: Medium complexity — consider refactoring if possible
- **11+**: High complexity — refactoring strongly recommended

Count +1 for each: `if`, `else if`, `while`, `for`, `case`, `catch`, `&&`, `||`, `?:`

### Cognitive Complexity
Measures how difficult code is to understand (not just count paths):
- Deep nesting increases cognitive load exponentially
- Recursion adds significant complexity
- Multiple return points increase complexity

## Analysis Template

For each class, provide:

```
Class: {ClassName}
Overall Complexity: Low | Medium | High
Reasoning: {1-2 sentence explanation}

Methods of concern:
- {methodName}: Cyclomatic={N}, Reason={why}

Suggestions:
- {Specific, actionable refactoring suggestion}
```

## Common Complexity Reduction Patterns

1. **Extract Method**: Break long methods into smaller focused ones
2. **Replace Conditional with Polymorphism**: Replace `if/else` type checks with strategy pattern
3. **Guard Clauses**: Return early to reduce nesting depth
4. **Replace Loop with Stream**: Use Java Stream API for collection processing
5. **Decompose Conditional**: Extract complex boolean expressions into named methods

## What NOT to flag as complex

- Simple getters/setters
- Builder pattern methods
- Single-line lambda expressions
- Record accessors
