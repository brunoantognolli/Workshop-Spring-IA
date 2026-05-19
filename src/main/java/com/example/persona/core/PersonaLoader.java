package com.example.persona.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link PersonaDefinition} instances from YAML files on the classpath.
 *
 * <p>Persona files follow the naming convention:
 * {@code classpath:/personas/{persona-id}-{version}.yaml}
 *
 * <p>Usage:
 * <pre>{@code
 * PersonaDefinition javadoc = personaLoader.load("javadoc-persona");
 * PersonaDefinition adrReviewer = personaLoader.load("adr-reviewer");
 * List<PersonaDefinition> all = personaLoader.loadAll();
 * }</pre>
 */
@Component
public class PersonaLoader {

    private static final Logger log = LoggerFactory.getLogger(PersonaLoader.class);
    private static final String PERSONAS_PATH = "classpath:/personas/*.yaml";

    private final ObjectMapper yamlMapper;
    private final PathMatchingResourcePatternResolver resourceResolver;

    public PersonaLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * Loads a persona by its ID. Scans {@code classpath:/personas/} for a file
     * whose {@code persona.id} field matches the given ID.
     *
     * @param personaId the ID declared in the YAML (e.g., "javadoc-persona")
     * @return the loaded PersonaDefinition
     * @throws PersonaLoadException if no matching persona is found or parsing fails
     */
    public PersonaDefinition load(String personaId) {
        return loadAll().stream()
                .filter(p -> p.id().equals(personaId))
                .findFirst()
                .orElseThrow(() -> new PersonaLoadException(
                        "No persona found with id: " + personaId));
    }

    /**
     * Loads all persona definitions from {@code classpath:/personas/*.yaml}.
     *
     * @return list of all available personas
     */
    public List<PersonaDefinition> loadAll() {
        try {
            Resource[] resources = resourceResolver.getResources(PERSONAS_PATH);
            List<PersonaDefinition> personas = new ArrayList<>();

            for (Resource resource : resources) {
                try {
                    PersonaDefinition persona = parsePersonaResource(resource);
                    personas.add(persona);
                    log.info("Loaded persona: {} v{}", persona.id(), persona.version());
                } catch (Exception e) {
                    log.warn("Failed to parse persona file {}: {}", resource.getFilename(), e.getMessage());
                }
            }

            return personas;
        } catch (IOException e) {
            throw new PersonaLoadException("Failed to scan personas directory", e);
        }
    }

    /**
     * Parses a single YAML resource. The YAML is expected to have a root {@code persona:} key.
     */
    private PersonaDefinition parsePersonaResource(Resource resource) throws IOException {
        JsonNode root = yamlMapper.readTree(resource.getInputStream());
        JsonNode personaNode = root.path("persona");

        if (personaNode.isMissingNode()) {
            throw new PersonaLoadException(
                    "YAML file " + resource.getFilename() + " is missing root 'persona:' key");
        }

        return yamlMapper.treeToValue(personaNode, PersonaDefinition.class);
    }

    /** Thrown when a persona YAML cannot be found or parsed. */
    public static class PersonaLoadException extends RuntimeException {
        public PersonaLoadException(String message) { super(message); }
        public PersonaLoadException(String message, Throwable cause) { super(message, cause); }
    }
}
