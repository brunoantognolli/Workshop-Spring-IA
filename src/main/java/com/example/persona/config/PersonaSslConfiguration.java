package com.example.persona.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Applies optional JVM TLS settings for local development on Windows/corporate networks.
 */
@Configuration
@EnableConfigurationProperties(PersonaSslProperties.class)
public class PersonaSslConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PersonaSslConfiguration.class);

    private final PersonaSslProperties sslProperties;

    public PersonaSslConfiguration(PersonaSslProperties sslProperties) {
        this.sslProperties = sslProperties;
    }

    @PostConstruct
    void applyTlsDevSettings() {
        if (sslProperties.isDisableRevocationCheck()) {
            System.setProperty("jdk.tls.disableRevocationCheck", "true");
            log.warn("persona.ssl.disable-revocation-check=true — TLS certificate revocation checks are DISABLED "
                    + "(intended for local dev only)");
        }
    }
}
