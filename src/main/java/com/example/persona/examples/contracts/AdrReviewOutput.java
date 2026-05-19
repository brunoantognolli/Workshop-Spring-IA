package com.example.persona.examples.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Output contract for the ADR Reviewer Persona.
 *
 * <p>This Java Record defines the strict JSON schema that the LLM must produce.
 * Referenced in {@code adr-reviewer-1.0.0.yaml} under {@code output_contract.schema}.
 */
public record AdrReviewOutput(

        @JsonProperty("adrTitle")
        String adrTitle,

        /**
         * One of: APPROVE | NEEDS_REVISION | REJECT
         */
        @JsonProperty("verdict")
        String verdict,

        @JsonProperty("tradeOffs")
        List<String> tradeOffs,

        @JsonProperty("concerns")
        List<String> concerns,

        @JsonProperty("recommendations")
        List<String> recommendations,

        @JsonProperty("summary")
        String summary
) {}
