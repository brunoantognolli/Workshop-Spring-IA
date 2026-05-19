package com.example.persona.guardrails;

import com.example.persona.core.PersonaDefinition.GuardrailDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BoundaryAdvisor}.
 * Uses Mockito to simulate the advisor chain — no LLM calls required.
 */
class BoundaryAdvisorTest {

    private ChatClientResponse mockResponseWith(String content) {
        AssistantMessage message = mock(AssistantMessage.class);
        when(message.getText()).thenReturn(content);

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(message);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        ChatClientResponse response = mock(ChatClientResponse.class);
        when(response.chatResponse()).thenReturn(chatResponse);

        return response;
    }

    private CallAdvisorChain mockChainReturning(ChatClientResponse response) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(response);
        return chain;
    }

    @Test
    @DisplayName("Should pass through when response has no forbidden patterns")
    void shouldPassCleanResponse() {
        List<GuardrailDef> guardrails = List.of(
                new GuardrailDef("content", "Response must not contain 'TODO' or 'FIXME'"));
        BoundaryAdvisor advisor = new BoundaryAdvisor(List.of(), guardrails);

        ChatClientResponse response = mockResponseWith("This is a clean response.");
        CallAdvisorChain chain = mockChainReturning(response);

        ChatClientResponse result = advisor.adviseCall(mock(ChatClientRequest.class), chain);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw BoundaryViolationException when 'TODO' is present")
    void shouldThrowOnTodoInResponse() {
        List<GuardrailDef> guardrails = List.of(
                new GuardrailDef("content", "Response must not contain 'TODO' or 'FIXME'"));
        BoundaryAdvisor advisor = new BoundaryAdvisor(List.of(), guardrails);

        ChatClientResponse response = mockResponseWith("This method needs TODO: implement later");
        CallAdvisorChain chain = mockChainReturning(response);

        assertThatThrownBy(() -> advisor.adviseCall(mock(ChatClientRequest.class), chain))
                .isInstanceOf(BoundaryViolationException.class)
                .hasMessageContaining("TODO");
    }

    @Test
    @DisplayName("Should throw BoundaryViolationException when 'FIXME' is present (case-insensitive)")
    void shouldThrowOnFixmeInResponseCaseInsensitive() {
        List<GuardrailDef> guardrails = List.of(
                new GuardrailDef("content", "Response must not contain 'FIXME'"));
        BoundaryAdvisor advisor = new BoundaryAdvisor(List.of(), guardrails);

        ChatClientResponse response = mockResponseWith("// fixme: this is broken");
        CallAdvisorChain chain = mockChainReturning(response);

        assertThatThrownBy(() -> advisor.adviseCall(mock(ChatClientRequest.class), chain))
                .isInstanceOf(BoundaryViolationException.class);
    }

    @Test
    @DisplayName("Should extract multiple forbidden patterns from a single rule")
    void shouldExtractMultiplePatternsFromOneRule() {
        List<GuardrailDef> guardrails = List.of(
                new GuardrailDef("content", "Must not contain 'SECRET' or 'PASSWORD' or 'TOKEN'"));
        BoundaryAdvisor advisor = new BoundaryAdvisor(List.of(), guardrails);

        // Response with "SECRET" should fail
        ChatClientResponse badResponse = mockResponseWith("The SECRET key is embedded here");
        assertThatThrownBy(() ->
                advisor.adviseCall(mock(ChatClientRequest.class), mockChainReturning(badResponse)))
                .isInstanceOf(BoundaryViolationException.class);

        // Clean response should pass
        ChatClientResponse goodResponse = mockResponseWith("The configuration value is set externally");
        assertThatNoException().isThrownBy(() ->
                advisor.adviseCall(mock(ChatClientRequest.class), mockChainReturning(goodResponse)));
    }

    @Test
    @DisplayName("Should have highest precedence order")
    void shouldHaveHighestPrecedenceOrder() {
        BoundaryAdvisor advisor = new BoundaryAdvisor(List.of(), List.of());
        assertThat(advisor.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}
