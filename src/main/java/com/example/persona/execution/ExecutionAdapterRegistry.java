package com.example.persona.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registry of all available {@link ExecutionAdapter} implementations.
 *
 * <p>Resolves the correct adapter for a given {@link ExecutionContext} by
 * iterating through registered adapters and returning the first one that
 * {@link ExecutionAdapter#supports(ExecutionContext) supports} the context.
 *
 * <p>Falls back to {@link ChatExecutionAdapter} if no specific adapter matches.
 */
@Component
public class ExecutionAdapterRegistry {

    private static final Logger log = LoggerFactory.getLogger(ExecutionAdapterRegistry.class);

    private final List<ExecutionAdapter> adapters;
    private final ExecutionAdapter defaultAdapter;

    /**
     * Spring injects all {@link ExecutionAdapter} beans into this constructor.
     * The order is determined by Spring's component scan — the first matching
     * adapter wins.
     */
    public ExecutionAdapterRegistry(List<ExecutionAdapter> adapters) {
        this.adapters = adapters;
        this.defaultAdapter = adapters.stream()
                .filter(a -> a instanceof ChatExecutionAdapter)
                .findFirst()
                .orElseGet(ChatExecutionAdapter::new);

        log.info("ExecutionAdapterRegistry initialized with {} adapters: {}",
                adapters.size(),
                adapters.stream().map(a -> a.getClass().getSimpleName()).toList());
    }

    /**
     * Resolves the appropriate adapter for the given execution context.
     *
     * @param context the execution context
     * @return the matching adapter, or {@link ChatExecutionAdapter} as default
     */
    public ExecutionAdapter resolve(ExecutionContext context) {
        ExecutionAdapter resolved = adapters.stream()
                .filter(a -> a.supports(context))
                .findFirst()
                .orElse(defaultAdapter);

        log.debug("Resolved adapter {} for mode {}",
                resolved.getClass().getSimpleName(), context.executionMode());
        return resolved;
    }
}
