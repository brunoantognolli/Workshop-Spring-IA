package com.example.persona.examples;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.examples.contracts.UnitTestGeneratorOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UnitTestGeneratorPersonaRunner}.
 * Fast, runs completely offline, mocks out the persona engine calls.
 */
class UnitTestGeneratorPersonaRunnerTest {

    private PersonaLoader loader;
    private PersonaEngine engine;
    private UnitTestGeneratorPersonaRunner runner;

    @BeforeEach
    void setUp() {
        loader = mock(PersonaLoader.class);
        engine = mock(PersonaEngine.class);
        runner = new UnitTestGeneratorPersonaRunner(loader, engine);
    }

    @Test
    @DisplayName("Should detect dynamic runtime environment and prepend to prompt")
    void shouldPrependDetectedEnvironmentContext() {
        PersonaDefinition mockPersona = mock(PersonaDefinition.class);
        when(loader.load("unit-test-generator")).thenReturn(mockPersona);
        
        UnitTestGeneratorOutput mockOutput = mock(UnitTestGeneratorOutput.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        when(engine.execute(eq(mockPersona), inputCaptor.capture())).thenReturn(mockOutput);

        String source = "public class OrderService {}";
        UnitTestGeneratorOutput result = runner.generateTests(source);

        assertThat(result).isSameAs(mockOutput);
        String capturedInput = inputCaptor.getValue();
        
        assertThat(capturedInput)
                .contains("# Project Environment Context")
                .contains("Java Version:")
                .contains("Spring Boot Version:")
                .contains("Spring Framework Version:")
                .contains("# Java Source Code Under Test")
                .contains(source);
    }

    @Test
    @DisplayName("Should use override target environment versions when provided")
    void shouldPrependSimulatedEnvironmentContext() {
        PersonaDefinition mockPersona = mock(PersonaDefinition.class);
        when(loader.load("unit-test-generator")).thenReturn(mockPersona);
        
        UnitTestGeneratorOutput mockOutput = mock(UnitTestGeneratorOutput.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        when(engine.execute(eq(mockPersona), inputCaptor.capture())).thenReturn(mockOutput);

        String source = "public class OrderService {}";
        UnitTestGeneratorOutput result = runner.generateTests(source, "1.7", "1.5.0.RELEASE");

        assertThat(result).isSameAs(mockOutput);
        String capturedInput = inputCaptor.getValue();
        
        assertThat(capturedInput)
                .contains("# Project Environment Context")
                .contains("- Java Version: 1.7")
                .contains("- Spring Boot Version: 1.5.0.RELEASE")
                .contains("Spring Framework Version:")
                .contains("# Java Source Code Under Test")
                .contains(source);
    }
}
