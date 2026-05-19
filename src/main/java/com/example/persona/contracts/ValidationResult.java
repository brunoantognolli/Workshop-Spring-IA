package com.example.persona.contracts;

import java.util.List;

/**
 * Result of validating a JSON string against an output contract schema.
 *
 * @param pass   true if the JSON is valid against the schema
 * @param errors list of human-readable validation error messages (empty if pass=true)
 */
public record ValidationResult(
        boolean pass,
        List<String> errors
) {
    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
