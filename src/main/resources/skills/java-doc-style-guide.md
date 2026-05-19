# Java Documentation Style Guide

This skill provides comprehensive guidelines for generating high-quality JavaDoc documentation
following Oracle standards and clean code principles.

## Core Principles

1. **Accuracy First**: Document only what the code does, never what you think it should do.
2. **Clarity**: Use simple, direct language. Avoid jargon unless domain-specific.
3. **Completeness**: Every public class, interface, and method must have documentation.

## Class-Level Documentation

A class JavaDoc should include:
- **One-line summary** ending with a period
- **Detailed description** (optional, 2-3 sentences max for PoC)
- **Usage example** for non-trivial classes

```java
/**
 * Manages the lifecycle of customer orders from creation to fulfillment.
 *
 * <p>This service handles order validation, inventory checks, and payment
 * processing. It coordinates with external services via event publishing.
 *
 * @author team-name
 * @version 1.0
 */
public class OrderService { ... }
```

## Method-Level Documentation

Every public method must document:
- **Summary line**: What it does (not how)
- **@param**: Each parameter with type description and valid values/ranges
- **@return**: What is returned (omit for void)
- **@throws**: Each checked exception with trigger condition

```java
/**
 * Creates a new order for the specified customer.
 *
 * @param customerId the unique identifier of the customer (must be positive)
 * @param items      list of order items; must not be null or empty
 * @return the created {@link Order} with a generated ID and PENDING status
 * @throws CustomerNotFoundException if no customer exists with the given ID
 * @throws InvalidOrderException     if items list is empty or contains invalid items
 */
public Order createOrder(long customerId, List<OrderItem> items) { ... }
```

## Complexity Analysis Guidelines

When analyzing code complexity:
- **Low**: Single responsibility, no nested conditionals, clear data flow
- **Medium**: 2-3 responsibilities, some branching, moderate coupling
- **High**: Multiple responsibilities, deep nesting, cross-cutting concerns

Suggest refactoring when cyclomatic complexity > 5 per method.

## Forbidden Patterns

Never include these in generated documentation:
- `TODO`, `FIXME`, `HACK`, `XXX` markers
- Implementation details that may change (e.g., "uses ArrayList internally")
- Emotional language ("unfortunately", "sadly")
- Redundant documentation (e.g., `/** Gets the name. */ String getName()`)
