package com.example.persona.examples;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.examples.contracts.UnitTestGeneratorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Demo runner for the Unit Test Generator Persona.
 *
 * <p>Demonstrates how to use the Persona Engine programmatically to generate unit tests.
 */
@Component
public class UnitTestGeneratorPersonaRunner {

    private static final Logger log = LoggerFactory.getLogger(UnitTestGeneratorPersonaRunner.class);
    private static final String PERSONA_ID = "unit-test-generator";

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;

    public UnitTestGeneratorPersonaRunner(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
    }

    /**
     * Generates a JUnit 5 / Mockito unit test class for a Java class using dynamically
     * detected active runtime versions of Java and Spring Boot.
     *
     * @param javaSourceCode the full Java source code of the class to test
     * @return structured unit test details following the {@link UnitTestGeneratorOutput} schema
     */
    public UnitTestGeneratorOutput generateTests(String javaSourceCode) {
        String javaVersion = System.getProperty("java.version");
        String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
        return generateTests(javaSourceCode, javaVersion, springBootVersion);
    }

    /**
     * Generates a JUnit 5 / Mockito unit test class for a Java class using overridden or
     * simulated Java and Spring Boot target versions.
     *
     * @param javaSourceCode            the full Java source code of the class to test
     * @param overrideJavaVersion       the target environment Java version (e.g. "8", "21")
     * @param overrideSpringBootVersion the target environment Spring Boot version (e.g. "2.7.0", "3.3.5")
     * @return structured unit test details following the {@link UnitTestGeneratorOutput} schema
     */
    public UnitTestGeneratorOutput generateTests(String javaSourceCode, String overrideJavaVersion, String overrideSpringBootVersion) {
        String springVersion = org.springframework.core.SpringVersion.getVersion();

        log.info("UnitTestGeneratorPersona: generating tests. Target Env: Java={}, Spring Boot={}",
                overrideJavaVersion, overrideSpringBootVersion);

        // Prepend the project environment context for the LLM to inspect
        StringBuilder enrichedInput = new StringBuilder();
        enrichedInput.append("# Project Environment Context\n")
                .append("- Java Version: ").append(overrideJavaVersion).append("\n")
                .append("- Spring Boot Version: ").append(overrideSpringBootVersion).append("\n")
                .append("- Spring Framework Version: ").append(springVersion).append("\n\n")
                .append("# Java Source Code Under Test\n")
                .append(javaSourceCode);

        PersonaDefinition persona = personaLoader.load(PERSONA_ID);
        Object result = personaEngine.execute(persona, enrichedInput.toString());
        return (UnitTestGeneratorOutput) result;
    }
}
