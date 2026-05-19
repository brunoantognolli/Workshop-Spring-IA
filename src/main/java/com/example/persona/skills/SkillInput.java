package com.example.persona.skills;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input type for skill tool callbacks.
 * The LLM sends a query when invoking a skill;
 * the skill returns its full Markdown content.
 */
public record SkillInput(
        @JsonProperty("query") String query
) {}
