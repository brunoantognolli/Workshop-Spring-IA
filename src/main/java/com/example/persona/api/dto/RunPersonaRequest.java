package com.example.persona.api.dto;

/**
 * Request body for {@code POST /api/personas/{id}/run}.
 *
 * @param input the raw text input sent to the persona (e.g., Java source code, ADR text)
 */
public record RunPersonaRequest(String input) {}
