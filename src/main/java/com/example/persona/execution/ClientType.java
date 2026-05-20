package com.example.persona.execution;

/**
 * Identifies the client or tool that is calling the persona API.
 *
 * <p>Used by the {@link ExecutionAdapter} layer to apply client-specific
 * formatting rules (e.g., Continue.dev expects different output format
 * than a raw API call).
 */
public enum ClientType {

    /** Continue.dev VS Code extension. */
    CONTINUE_DEV,

    /** Cursor IDE. */
    CURSOR,

    /** Direct API call (e.g., curl, Postman, custom integration). */
    API,

    /** Command-line interface client. */
    CLI,

    /** Unknown or undetected client. */
    UNKNOWN
}
