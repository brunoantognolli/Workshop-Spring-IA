package com.example.persona.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ExecutionAdapterRegistry}.
 * Verifies that the correct adapter is resolved for each execution mode.
 */
class ExecutionAdapterRegistryTest {

    private ExecutionAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        List<ExecutionAdapter> adapters = List.of(
                new ChatExecutionAdapter(),
                new IdeEditExecutionAdapter(),
                new IdeApplyExecutionAdapter(),
                new JsonApiExecutionAdapter()
        );
        registry = new ExecutionAdapterRegistry(adapters);
    }

    @Test
    @DisplayName("Should resolve ChatExecutionAdapter for CHAT mode")
    void shouldResolveChatAdapter() {
        ExecutionContext context = buildContext(ExecutionMode.CHAT);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(ChatExecutionAdapter.class);
    }

    @Test
    @DisplayName("Should resolve ChatExecutionAdapter for ANALYSIS mode")
    void shouldResolveChatAdapterForAnalysis() {
        ExecutionContext context = buildContext(ExecutionMode.ANALYSIS);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(ChatExecutionAdapter.class);
    }

    @Test
    @DisplayName("Should resolve ChatExecutionAdapter for CI_REVIEW mode")
    void shouldResolveChatAdapterForCiReview() {
        ExecutionContext context = buildContext(ExecutionMode.CI_REVIEW);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(ChatExecutionAdapter.class);
    }

    @Test
    @DisplayName("Should resolve IdeEditExecutionAdapter for IDE_EDIT mode")
    void shouldResolveIdeEditAdapter() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_EDIT);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(IdeEditExecutionAdapter.class);
    }

    @Test
    @DisplayName("Should resolve IdeApplyExecutionAdapter for IDE_APPLY mode")
    void shouldResolveIdeApplyAdapter() {
        ExecutionContext context = buildContext(ExecutionMode.IDE_APPLY);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(IdeApplyExecutionAdapter.class);
    }

    @Test
    @DisplayName("Should resolve JsonApiExecutionAdapter for JSON_API mode")
    void shouldResolveJsonApiAdapter() {
        ExecutionContext context = buildContext(ExecutionMode.JSON_API);
        ExecutionAdapter adapter = registry.resolve(context);

        assertThat(adapter).isInstanceOf(JsonApiExecutionAdapter.class);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ExecutionContext buildContext(ExecutionMode mode) {
        return new ExecutionContextBuilder()
                .personaId("test-persona")
                .personaVersion("1.0.0")
                .executionMode(mode)
                .userInstruction("test input")
                .build();
    }
}
