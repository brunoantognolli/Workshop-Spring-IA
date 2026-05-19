package com.example.persona.examples.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Output contract for the JavaDoc Persona.
 *
 * <p>This Java Record defines the strict JSON schema that the LLM must produce.
 * Referenced in {@code javadoc-persona-1.0.0.yaml} under {@code output_contract.schema}.
 */
public record JavaDocOutput(

        @JsonProperty("className")
        String className,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("methods")
        List<MethodDoc> methods,

        @JsonProperty("complexity")
        ComplexityReport complexity
) {

    /**
     * Documentation for a single Java method.
     */
    public record MethodDoc(
            @JsonProperty("name")        String name,
            @JsonProperty("signature")   String signature,
            @JsonProperty("description") String description,
            @JsonProperty("params")      List<String> params,
            @JsonProperty("returns")     String returns
    ) {}

    /**
     * Complexity analysis summary for the documented class.
     */
    public record ComplexityReport(
            @JsonProperty("overall")     String overall,
            @JsonProperty("suggestions") List<String> suggestions
    ) {}
}
