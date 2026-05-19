package com.example.persona.api;

import com.example.persona.api.dto.RunPersonaRequest;
import com.example.persona.api.dto.RunPersonaResponse;
import com.example.persona.core.PersonaDefinition;
import com.example.persona.core.PersonaEngine;
import com.example.persona.core.PersonaLoader;
import com.example.persona.guardrails.BoundaryViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for running personas.
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code GET  /api/personas}         — list all available personas</li>
 *   <li>{@code GET  /api/personas/{id}}     — get persona definition by ID</li>
 *   <li>{@code POST /api/personas/{id}/run} — execute a persona against input</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private static final Logger log = LoggerFactory.getLogger(PersonaController.class);

    private final PersonaLoader personaLoader;
    private final PersonaEngine personaEngine;

    public PersonaController(PersonaLoader personaLoader, PersonaEngine personaEngine) {
        this.personaLoader = personaLoader;
        this.personaEngine = personaEngine;
    }

    /** Lists all available personas. */
    @GetMapping
    public List<Map<String, String>> listPersonas() {
        return personaLoader.loadAll().stream()
                .map(p -> Map.of(
                        "id", p.id(),
                        "version", p.version(),
                        "role", p.role()))
                .toList();
    }

    /** Returns a persona definition by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPersona(@PathVariable String id) {
        try {
            PersonaDefinition persona = personaLoader.load(id);
            return ResponseEntity.ok(persona);
        } catch (PersonaLoader.PersonaLoadException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Executes a persona against the given input.
     *
     * <p>Example request:
     * <pre>{@code
     * POST /api/personas/javadoc-persona/run
     * { "input": "public class OrderService { ... }" }
     * }</pre>
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<?> runPersona(@PathVariable String id,
                                        @RequestBody RunPersonaRequest request) {
        log.info("Running persona '{}' with input length={}", id, request.input().length());

        try {
            PersonaDefinition persona = personaLoader.load(id);
            Object output = personaEngine.execute(persona, request.input());

            RunPersonaResponse response = new RunPersonaResponse(id, output, "ollama/mistral", false);
            return ResponseEntity.ok(response);

        } catch (PersonaLoader.PersonaLoadException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Persona not found: " + id));

        } catch (BoundaryViolationException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "Boundary violation", "rule", e.getViolatedRule()));

        } catch (Exception e) {
            log.error("Persona execution failed for '{}': {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Execution failed: " + e.getMessage()));
        }
    }
}
