package com.example.persona.examples.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Output contract for the Unit Test Generator Persona.
 *
 * <p>This Java Record defines the strict JSON schema that the LLM must produce.
 * Referenced in {@code unit-test-generator-1.0.0.yaml} under {@code output_contract.schema}.
 */
public record UnitTestGeneratorOutput(

        @JsonProperty("testClassName")
        String testClassName,

        @JsonProperty("targetClassName")
        String targetClassName,

        @JsonProperty("imports")
        List<String> imports,

        @JsonProperty("classAnnotations")
        List<String> classAnnotations,

        @JsonProperty("fields")
        List<TestField> fields,

        @JsonProperty("testMethods")
        List<TestMethod> testMethods,

        @JsonProperty("fullClassCode")
        String fullClassCode,

        @JsonProperty("unsupportedMessage")
        String unsupportedMessage
) {
    @com.fasterxml.jackson.annotation.JsonCreator
    public UnitTestGeneratorOutput {}

    /**
     * Represents a mock, spy, or injectMocks field declaration.
     */
    public record TestField(
            @JsonProperty("annotations") List<String> annotations,
            @JsonProperty("type")        String type,
            @JsonProperty("name")        String name
    ) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public TestField {}
    }

    /**
     * Represents an individual unit test method.
     */
    public record TestMethod(
            @JsonProperty("name")        String name,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("annotations") List<String> annotations,
            @JsonProperty("code")        String code
    ) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public TestMethod {}
    }
}
