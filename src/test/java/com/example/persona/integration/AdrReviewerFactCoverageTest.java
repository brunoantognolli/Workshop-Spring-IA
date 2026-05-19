package com.example.persona.integration;

import com.example.persona.examples.AdrReviewerPersonaRunner;
import com.example.persona.examples.contracts.AdrReviewOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the ADR Reviewer Persona.
 *
 * <h3>Requires:</h3>
 * <ul>
 *   <li>Ollama running locally on port 11434 with mistral pulled</li>
 * </ul>
 *
 * <p>Run with: {@code mvn test -Dgroups="integration"}
 */
@SpringBootTest
@Tag("integration")
class AdrReviewerFactCoverageTest {

    private static final String SAMPLE_ADR = """
            # ADR-007: Adopt PostgreSQL as the Primary Database

            **Status**: Proposed
            **Date**: 2025-05-15
            **Deciders**: Architecture Team

            ## Context
            Our current application uses an in-memory H2 database for development and testing,
            but we need a production-grade relational database that supports concurrent users,
            ACID transactions, and full-text search capabilities.

            ## Decision
            We will adopt PostgreSQL 16 as our primary relational database for all environments,
            replacing H2 in development.

            ## Consequences
            ### Positive
            - ACID compliance ensures data integrity under concurrent load
            - Rich ecosystem of monitoring tools

            ### Negative
            - Local development setup becomes more complex (requires Docker or local install)
            - H2 in-memory tests must be rewritten for PostgreSQL dialect

            ### Risks
            - Team familiarity with PostgreSQL-specific features
            """;

    @Autowired
    private AdrReviewerPersonaRunner adrReviewerPersonaRunner;

    @Test
    @DisplayName("ADR review verdict must be one of APPROVE, NEEDS_REVISION, or REJECT")
    void verdictMustBeValid() {
        AdrReviewOutput review = adrReviewerPersonaRunner.review(SAMPLE_ADR);
        assertThat(review.verdict()).isIn("APPROVE", "NEEDS_REVISION", "REJECT");
    }

    @Test
    @DisplayName("ADR review must identify at least one trade-off (persona boundary)")
    void mustIdentifyTradeOffs() {
        AdrReviewOutput review = adrReviewerPersonaRunner.review(SAMPLE_ADR);
        assertThat(review.tradeOffs()).isNotEmpty();
    }

    @Test
    @DisplayName("ADR review summary should reference PostgreSQL (grounding check)")
    void summaryShouldReferenceSubject() {
        AdrReviewOutput review = adrReviewerPersonaRunner.review(SAMPLE_ADR);
        assertThat(review.summary().toLowerCase())
                .as("Summary must reference 'postgresql' — the subject of the ADR")
                .containsIgnoringCase("postgresql");
    }

    @Test
    @DisplayName("ADR title should be captured from the input")
    void titleShouldBeExtracted() {
        AdrReviewOutput review = adrReviewerPersonaRunner.review(SAMPLE_ADR);
        assertThat(review.adrTitle())
                .as("ADR title should be populated")
                .isNotBlank();
    }

    @Test
    @DisplayName("Well-formed ADR with trade-offs should not be REJECTED")
    void wellFormedAdrShouldNotBeRejected() {
        AdrReviewOutput review = adrReviewerPersonaRunner.review(SAMPLE_ADR);
        assertThat(review.verdict()).isNotEqualTo("REJECT");
    }
}
