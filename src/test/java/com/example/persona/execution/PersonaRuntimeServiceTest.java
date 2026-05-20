package com.example.persona.execution;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PersonaRuntimeService} wiring to {@link PersonaEngine}.
 */
@ExtendWith(MockitoExtension.class)
class PersonaRuntimeServiceTest {

    @Mock
    private PersonaLoader personaLoader;

    @Mock
    private PersonaEngine personaEngine;

    private PersonaRuntimeService runtimeService;

    private PersonaDefinition personaWithContract;

    @BeforeEach
    void setUp() {
        ExecutionAdapterRegistry registry = new ExecutionAdapterRegistry(List.of(
                new ChatExecutionAdapter(),
                new IdeEditExecutionAdapter(),
                new IdeApplyExecutionAdapter(),
                new JsonApiExecutionAdapter()
        ));
        runtimeService = new PersonaRuntimeService(personaLoader, personaEngine, registry);

        personaWithContract = new PersonaDefinition(
                "unit-test-generator", "1.0.0", "You generate unit tests.",
                null, List.of(), List.of(), List.of(),
                new PersonaDefinition.OutputContractDef("json", "com.example.SomeOutput"),
                List.of(), null
        );
    }

    @Test
    @DisplayName("Should pass IDE edit adapted system prompt to PersonaEngine")
    void shouldPassAdaptedPromptToEngineForIdeEdit() {
        when(personaLoader.load("unit-test-generator")).thenReturn(personaWithContract);
        when(personaEngine.buildSystemPrompt(eq(personaWithContract), eq("")))
                .thenReturn("# Role\nYou generate unit tests.");
        when(personaEngine.executeWithSystemPrompt(eq(personaWithContract), eq("public class Foo {}"),
                eq(true), anyString()))
                .thenReturn("public class Foo { }");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        String result = runtimeService.execute("unit-test-generator:edit", "public class Foo {}");

        verify(personaEngine).executeWithSystemPrompt(eq(personaWithContract), eq("public class Foo {}"),
                eq(true), promptCaptor.capture());
        assertThat(result).isEqualTo("public class Foo { }");
        assertThat(promptCaptor.getValue())
                .contains("IDE Edit")
                .contains("Do not explain")
                .contains("Do not return JSON");
    }

    @Test
    @DisplayName("Should pass IDE apply adapted system prompt without JSON output format section")
    void shouldPassAdaptedPromptToEngineForIdeApply() {
        when(personaLoader.load("unit-test-generator")).thenReturn(personaWithContract);
        when(personaEngine.buildSystemPrompt(eq(personaWithContract), eq("")))
                .thenReturn("# Role\nYou generate unit tests.\n\n# Output Format\n{\"schema\": true}");
        when(personaEngine.executeWithSystemPrompt(eq(personaWithContract), eq("refactor this"),
                eq(true), anyString()))
                .thenReturn("public class Foo {}");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        runtimeService.execute("unit-test-generator:apply", "refactor this");

        verify(personaEngine).executeWithSystemPrompt(eq(personaWithContract), eq("refactor this"),
                eq(true), promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .doesNotContain("# Output Format")
                .doesNotContain("{\"schema\": true}")
                .contains("IDE Apply")
                .contains("Do not return JSON under any circumstances");
    }

    @Test
    @DisplayName("Should pass JSON API adapted prompt and keep output contract enforcement")
    void shouldPassAdaptedPromptToEngineForJsonApi() {
        when(personaLoader.load("unit-test-generator")).thenReturn(personaWithContract);
        when(personaEngine.buildSystemPrompt(eq(personaWithContract), anyString()))
                .thenReturn("# Role\nYou generate unit tests.\n\n# Output Format\nschema-here");
        when(personaEngine.executeWithSystemPrompt(eq(personaWithContract), eq("input"),
                eq(false), anyString()))
                .thenReturn("{\"ok\": true}");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        runtimeService.execute("unit-test-generator:json", "input");

        verify(personaEngine).executeWithSystemPrompt(eq(personaWithContract), eq("input"),
                eq(false), promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("JSON API")
                .contains("valid JSON");
    }
}
