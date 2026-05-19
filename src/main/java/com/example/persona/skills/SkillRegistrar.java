package com.example.persona.skills;

import com.example.persona.core.PersonaDefinition.SkillDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts Markdown skill files into Spring AI {@link ToolCallback} definitions.
 *
 * <p>Each skill file is a {@code .md} document on the classpath. When the LLM decides
 * to invoke a skill, it receives the full Markdown content as the tool response.
 *
 * <h3>How skills work:</h3>
 * <ol>
 *   <li>The Markdown file is loaded at registration time</li>
 *   <li>The first non-empty paragraph becomes the tool {@code description}</li>
 *   <li>The LLM calls the tool with a natural language {@code query}</li>
 *   <li>The full Markdown content is returned to the LLM as context</li>
 * </ol>
 */
@Component
public class SkillRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistrar.class);

    /**
     * Registers a list of skills as ToolCallback definitions.
     *
     * @param skills list of skill definitions from the persona YAML
     * @return list of ToolCallbacks ready to pass to ChatClient.tools(...)
     */
    public List<ToolCallback> registerSkills(List<SkillDef> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        List<ToolCallback> callbacks = new ArrayList<>();
        for (SkillDef skill : skills) {
            try {
                String markdownContent = loadMarkdown(skill.path());
                String description = extractDescription(markdownContent, skill.id());

                // Use FunctionToolCallback — the Spring AI 1.1.x lambda-based tool builder
                ToolCallback callback = FunctionToolCallback
                        .<SkillInput, String>builder(skill.id(), (SkillInput input) -> markdownContent)
                        .description(description)
                        .inputType(SkillInput.class)
                        .build();

                callbacks.add(callback);
                log.debug("Registered skill tool: {}", skill.id());

            } catch (IOException e) {
                log.warn("Could not load skill '{}' from path '{}': {}",
                        skill.id(), skill.path(), e.getMessage());
            }
        }
        return callbacks;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves a classpath path like {@code classpath:/skills/foo.md} and reads it.
     */
    private String loadMarkdown(String path) throws IOException {
        String resourcePath = path.replace("classpath:", "").replaceFirst("^/", "");
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Extracts the first meaningful paragraph from the Markdown content as the tool description.
     * Falls back to the skill ID if no paragraph is found.
     */
    private String extractDescription(String markdown, String fallbackId) {
        return Arrays.stream(markdown.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .findFirst()
                .orElse("Skill: " + fallbackId);
    }
}
