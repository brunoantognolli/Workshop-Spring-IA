package com.example.persona.examples;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.examples.contracts.AdrReviewOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Demo runner for the ADR Reviewer Persona.
 *
 * <p>Demonstrates how a second persona (ADR Reviewer) is added to the system
 * with zero changes to the engine — only a new YAML + Record.
 *
 * <p>Example:
 * <pre>{@code
 * @Autowired AdrReviewerPersonaRunner runner;
 * AdrReviewOutput review = runner.review(adrMarkdownText);
 * }</pre>
 */
@Component
public class AdrReviewerPersonaRunner {

    private static final Logger log = LoggerFactory.getLogger(AdrReviewerPersonaRunner.class);
    private static final String PERSONA_ID = "adr-reviewer";

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;

    public AdrReviewerPersonaRunner(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
    }

    /**
     * Reviews an Architecture Decision Record (ADR).
     *
     * @param adrText the full ADR text in Markdown format
     * @return structured review following the {@link AdrReviewOutput} schema
     */
    public AdrReviewOutput review(String adrText) {
        log.info("AdrReviewerPersona: reviewing ADR ({} chars)", adrText.length());
        PersonaDefinition persona = personaLoader.load(PERSONA_ID);
        Object result = personaEngine.execute(persona, adrText);
        return (AdrReviewOutput) result;
    }
}
