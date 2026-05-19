package com.example.persona.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * OpenAI-compatible model list response.
 * Returned by {@code GET /v1/models} — lists all available personas as "models".
 */
public record ModelListResponse(
        String object,
        List<ModelObject> data
) {

    public record ModelObject(
            String id,
            String object,
            long created,
            @JsonProperty("owned_by") String ownedBy
    ) {}

    /** Builds a response from a list of persona IDs. */
    public static ModelListResponse of(List<String> personaIds) {
        long now = Instant.now().getEpochSecond();
        List<ModelObject> models = personaIds.stream()
                .map(id -> new ModelObject(id, "model", now, "persona-engine"))
                .toList();
        return new ModelListResponse("list", models);
    }
}
