package com.example.persona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Persona as Code — Industrial PoC Entry Point.
 *
 * <p>Exposes two APIs:
 * <ul>
 *   <li>{@code /api/personas/**} — REST API for running personas</li>
 *   <li>{@code /v1/**} — OpenAI-compatible API (usable from Cursor, Continue.dev, Open WebUI)</li>
 * </ul>
 */
@SpringBootApplication
public class PersonaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonaApplication.class, args);
    }
}
