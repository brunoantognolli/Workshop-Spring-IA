package com.example.persona.examples;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.examples.contracts.JavaDocOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Demo runner for the JavaDoc Persona.
 *
 * <p>Demonstrates how to use the Persona Engine programmatically from Java code.
 * This class can also be used by integration tests.
 *
 * <p>Example:
 * <pre>{@code
 * @Autowired JavaDocPersonaRunner runner;
 * JavaDocOutput doc = runner.document("public class Foo { public void bar() {} }");
 * }</pre>
 */
@Component
public class JavaDocPersonaRunner {

    private static final Logger log = LoggerFactory.getLogger(JavaDocPersonaRunner.class);
    private static final String PERSONA_ID = "javadoc-persona";

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;

    public JavaDocPersonaRunner(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
    }

    /**
     * Documents a Java class from its source code.
     *
     * @param javaSourceCode the full Java source code to document
     * @return structured documentation following the {@link JavaDocOutput} schema
     */
    public JavaDocOutput document(String javaSourceCode) {
        log.info("JavaDocPersona: documenting {} chars of Java source", javaSourceCode.length());
        PersonaDefinition persona = personaLoader.load(PERSONA_ID);
        Object result = personaEngine.execute(persona, javaSourceCode);
        return (JavaDocOutput) result;
    }
}
