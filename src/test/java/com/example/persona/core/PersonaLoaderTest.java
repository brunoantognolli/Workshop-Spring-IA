package com.example.persona.core;

import com.example.persona.core.PersonaDefinition.GuardrailDef;
import com.example.persona.core.PersonaDefinition.SkillDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PersonaLoader}.
 * No LLM calls required — tests only YAML parsing.
 */
class PersonaLoaderTest {

    private PersonaLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PersonaLoader();
    }

    @Test
    @DisplayName("Should load javadoc-persona with all required fields")
    void shouldLoadJavaDocPersona() {
        PersonaDefinition persona = loader.load("javadoc-persona");

        assertThat(persona.id()).isEqualTo("javadoc-persona");
        assertThat(persona.version()).isEqualTo("1.0.0");
        assertThat(persona.role()).isNotBlank();
        assertThat(persona.skills()).hasSize(2);
        assertThat(persona.boundaries()).isNotEmpty();
        assertThat(persona.guardrails()).isNotEmpty();
        assertThat(persona.outputContract()).isNotNull();
        assertThat(persona.outputContract().schema())
                .isEqualTo("com.example.persona.examples.contracts.JavaDocOutput");
    }

    @Test
    @DisplayName("Should load adr-reviewer persona with all required fields")
    void shouldLoadAdrReviewerPersona() {
        PersonaDefinition persona = loader.load("adr-reviewer");

        assertThat(persona.id()).isEqualTo("adr-reviewer");
        assertThat(persona.skills()).hasSize(2);
        assertThat(persona.outputContract().schema())
                .isEqualTo("com.example.persona.examples.contracts.AdrReviewOutput");
    }

    @Test
    @DisplayName("Should load both personas via loadAll()")
    void shouldLoadAllPersonas() {
        List<PersonaDefinition> all = loader.loadAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
        assertThat(all).extracting(PersonaDefinition::id)
                .containsExactlyInAnyOrder("javadoc-persona", "adr-reviewer");
    }

    @Test
    @DisplayName("Should parse skill paths correctly")
    void shouldParseSkillPaths() {
        PersonaDefinition persona = loader.load("javadoc-persona");

        List<String> skillIds = persona.skills().stream()
                .map(SkillDef::id).toList();
        assertThat(skillIds).contains("java_doc_style_guide", "java_complexity_analysis");

        persona.skills().forEach(skill ->
                assertThat(skill.path()).startsWith("classpath:/skills/"));
    }

    @Test
    @DisplayName("Should parse guardrail rules correctly")
    void shouldParseGuardrails() {
        PersonaDefinition persona = loader.load("javadoc-persona");

        List<String> rules = persona.guardrails().stream()
                .map(GuardrailDef::rule).toList();
        assertThat(rules).anyMatch(r -> r.contains("TODO") || r.contains("FIXME"));
    }

    @Test
    @DisplayName("Should throw PersonaLoadException for unknown persona ID")
    void shouldThrowForUnknownPersonaId() {
        assertThatThrownBy(() -> loader.load("non-existent-persona"))
                .isInstanceOf(PersonaLoader.PersonaLoadException.class)
                .hasMessageContaining("non-existent-persona");
    }
}
