package com.example.persona.integration;

import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class PersonaMemoryIntegrationTest {

    @Autowired
    private PersonaEngine personaEngine;

    @Autowired
    private PersonaLoader personaLoader;

    @Test
    @DisplayName("Should remember user name across multiple calls with same conversationId")
    void shouldRememberContextAcrossCalls() {
        PersonaDefinition javadoc = personaLoader.load("javadoc-persona");
        String convId = UUID.randomUUID().toString();
        String userId = "bruno";

        // Call 1: Tell my name
        String response1 = (String) personaEngine.execute(javadoc, "Meu nome é Bruno. Lembre-se disso. Responda apenas: Olá Bruno", true, convId, userId);
        assertThat(response1).containsIgnoringCase("Bruno");

        // Call 2: Ask my name
        String response2 = (String) personaEngine.execute(javadoc, "Qual é o meu nome? Responda de forma curta.", true, convId, userId);
        assertThat(response2).containsIgnoringCase("Bruno");
    }

    @Test
    @DisplayName("Should not remember context if conversationId is different")
    void shouldNotRememberIfDifferentConversation() {
        PersonaDefinition javadoc = personaLoader.load("javadoc-persona");
        String convId1 = UUID.randomUUID().toString();
        String convId2 = UUID.randomUUID().toString();
        String userId = "bruno";

        // Call 1 (Conversation 1): Tell my name
        personaEngine.execute(javadoc, "Meu nome é Bruno. Lembre-se disso.", true, convId1, userId);

        // Call 2 (Conversation 2): Ask my name
        String response = (String) personaEngine.execute(javadoc, "Qual é o meu nome?", true, convId2, userId);
        assertThat(response).doesNotContain("Bruno");
    }

    @Test
    @DisplayName("Should not mix memory across different personas even with same conversationId")
    void shouldNotMixAcrossPersonas() {
        PersonaDefinition javadoc = personaLoader.load("javadoc-persona");
        PersonaDefinition adr = personaLoader.load("adr-reviewer");
        String convId = UUID.randomUUID().toString();
        String userId = "bruno";

        // Call 1 (javadoc): Tell my name
        personaEngine.execute(javadoc, "Meu nome é Bruno. Lembre-se disso.", true, convId, userId);

        // Call 2 (adr-reviewer): Ask my name
        String response = (String) personaEngine.execute(adr, "Qual é o meu nome?", true, convId, userId);
        assertThat(response).doesNotContain("Bruno");
    }
}
