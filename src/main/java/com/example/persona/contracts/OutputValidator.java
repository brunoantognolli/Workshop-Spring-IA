package com.example.persona.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates a JSON string against the JSON Schema derived from a Java Record.
 *
 * <p>Uses {@link BeanOutputConverter} to generate the schema from the Java Record,
 * then validates the LLM response using the NetworkNT JSON Schema Validator.
 *
 * <p>Schema instances are cached per output class for performance.
 */
@Component
public class OutputValidator {

    private static final Logger log = LoggerFactory.getLogger(OutputValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<Class<?>, JsonSchema> schemaCache;

    public OutputValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.schemaCache = new ConcurrentHashMap<>();
    }

    /**
     * Validates whether the given JSON string conforms to the schema of the output class.
     *
     * @param jsonResponse the raw JSON string from the LLM
     * @param outputClass  the Java Record class representing the expected schema
     * @return {@link ValidationResult} with pass/fail and error messages
     */
    public ValidationResult validate(String jsonResponse, Class<?> outputClass) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return ValidationResult.fail(List.of("Response is null or empty"));
        }

        try {
            JsonNode responseNode = objectMapper.readTree(extractJson(jsonResponse));
            JsonSchema schema = getOrBuildSchema(outputClass);
            Set<ValidationMessage> violations = schema.validate(responseNode);

            if (violations.isEmpty()) {
                log.debug("Output contract validation PASSED for {}", outputClass.getSimpleName());
                return ValidationResult.ok();
            }

            List<String> errors = violations.stream()
                    .map(ValidationMessage::getMessage)
                    .toList();
            log.warn("Output contract validation FAILED for {}: {}", outputClass.getSimpleName(), errors);
            return ValidationResult.fail(errors);

        } catch (Exception e) {
            log.warn("Could not parse LLM response as JSON: {}", e.getMessage());
            return ValidationResult.fail(List.of("Response is not valid JSON: " + e.getMessage()));
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Retrieves or builds and caches the JSON Schema for the given class.
     */
    private JsonSchema getOrBuildSchema(Class<?> outputClass) {
        return schemaCache.computeIfAbsent(outputClass, clazz -> {
            BeanOutputConverter<?> converter = new BeanOutputConverter<>(clazz);
            String jsonSchemaStr = converter.getJsonSchema();
            log.debug("Building JSON schema for {}", clazz.getSimpleName());
            return schemaFactory.getSchema(jsonSchemaStr);
        });
    }

    /**
     * Extracts a JSON block from a response that may contain markdown code fences.
     * Example: ```json { ... } ``` → { ... }
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
