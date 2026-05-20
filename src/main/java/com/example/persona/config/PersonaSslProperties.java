package com.example.persona.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local-dev SSL options for outbound HTTPS to cloud LLM providers (e.g. Groq).
 */
@ConfigurationProperties(prefix = "persona.ssl")
public class PersonaSslProperties {

    /**
     * When true, sets {@code jdk.tls.disableRevocationCheck=true} at startup.
     * Useful on Windows when Schannel/Java fail with CRYPT_E_NO_REVOCATION_CHECK or PKIX errors.
     * Do not enable in production unless you understand the security trade-off.
     */
    private boolean disableRevocationCheck = false;

    public boolean isDisableRevocationCheck() {
        return disableRevocationCheck;
    }

    public void setDisableRevocationCheck(boolean disableRevocationCheck) {
        this.disableRevocationCheck = disableRevocationCheck;
    }
}
