package com.example.persona.guardrails;

/**
 * Thrown when a persona response violates a declared boundary or guardrail rule.
 */
public class BoundaryViolationException extends RuntimeException {

    private final String violatedRule;
    private final String offendingContent;

    public BoundaryViolationException(String violatedRule, String offendingContent) {
        super("Boundary violation — Rule: [" + violatedRule + "] | Detected in response.");
        this.violatedRule = violatedRule;
        this.offendingContent = offendingContent;
    }

    public String getViolatedRule() { return violatedRule; }
    public String getOffendingContent() { return offendingContent; }
}
