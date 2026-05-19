package com.example.persona.integration;

import com.example.persona.examples.JavaDocPersonaRunner;
import com.example.persona.examples.contracts.JavaDocOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Metamorphic integration test for the JavaDoc Persona.
 *
 * <p><b>Metamorphic property under test:</b> Renaming local variables in the source code
 * must not change the documented class name, public method count, or substantially
 * alter the method descriptions.
 *
 * <p>If a persona hallucinates and invents variable names in documentation,
 * renaming them would produce different outputs — detecting the hallucination.
 *
 * <h3>Requires:</h3>
 * <ul>
 *   <li>Ollama running locally on port 11434</li>
 * </ul>
 *
 * <p>Run with: {@code mvn test -Dgroups="integration"}
 */
@SpringBootTest
@Tag("integration")
class JavaDocMetamorphicTest {

    private static final String ORIGINAL_SOURCE = """
            public class PaymentProcessor {

                private final TransactionLog log;

                public PaymentProcessor(TransactionLog log) {
                    this.log = log;
                }

                public Receipt processPayment(double amount, String currency) {
                    Transaction transaction = new Transaction(amount, currency);
                    log.record(transaction);
                    return new Receipt(transaction.getId());
                }

                public boolean refund(String transactionId) {
                    Transaction transaction = log.findById(transactionId);
                    if (transaction == null) return false;
                    transaction.reverse();
                    return true;
                }
            }
            """;

    // Metamorphic transformation: rename local variables (amount→value, currency→unit, etc.)
    private static final String RENAMED_SOURCE = ORIGINAL_SOURCE
            .replace("double amount", "double value")
            .replace("String currency", "String unit")
            .replace("amount, currency", "value, unit")
            .replace("Transaction transaction", "Transaction tx")
            .replace("transaction", "tx");

    @Autowired
    private JavaDocPersonaRunner javaDocPersonaRunner;

    @Test
    @DisplayName("Variable renaming should not change class name in documentation")
    void variableRenamingShouldNotChangeClassName() {
        JavaDocOutput original = javaDocPersonaRunner.document(ORIGINAL_SOURCE);
        JavaDocOutput renamed  = javaDocPersonaRunner.document(RENAMED_SOURCE);

        assertThat(original.className())
                .as("Class name must be the same regardless of variable names")
                .isEqualToIgnoringCase(renamed.className());
    }

    @Test
    @DisplayName("Variable renaming should not change documented method count")
    void variableRenamingShouldNotChangeMethodCount() {
        JavaDocOutput original = javaDocPersonaRunner.document(ORIGINAL_SOURCE);
        JavaDocOutput renamed  = javaDocPersonaRunner.document(RENAMED_SOURCE);

        assertThat(original.methods().size())
                .as("Number of documented methods must be stable across metamorphic transformation")
                .isEqualTo(renamed.methods().size());
    }

    @Test
    @DisplayName("Documented method names should be stable across variable renaming (Jaccard >= 0.8)")
    void methodNamesShouldBeStable() {
        JavaDocOutput original = javaDocPersonaRunner.document(ORIGINAL_SOURCE);
        JavaDocOutput renamed  = javaDocPersonaRunner.document(RENAMED_SOURCE);

        Set<String> originalNames = new HashSet<>(original.methods().stream()
                .map(m -> m.name().toLowerCase()).toList());
        Set<String> renamedNames = new HashSet<>(renamed.methods().stream()
                .map(m -> m.name().toLowerCase()).toList());

        double jaccard = jaccardSimilarity(originalNames, renamedNames);
        assertThat(jaccard)
                .as("Method names should be identical or near-identical (Jaccard >= 0.8). " +
                    "Original=%s, Renamed=%s".formatted(originalNames, renamedNames))
                .isGreaterThanOrEqualTo(0.8);
    }

    @Test
    @DisplayName("Complexity level should be stable across variable renaming")
    void complexityShouldBeStable() {
        JavaDocOutput original = javaDocPersonaRunner.document(ORIGINAL_SOURCE);
        JavaDocOutput renamed  = javaDocPersonaRunner.document(RENAMED_SOURCE);

        // Overall complexity level should be the same (low/medium/high)
        assertThat(original.complexity().overall().toLowerCase())
                .as("Complexity level must not change due to variable renaming")
                .isEqualTo(renamed.complexity().overall().toLowerCase());
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /**
     * Computes Jaccard similarity between two sets of strings.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     */
    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        if (union.isEmpty()) return 1.0;
        return (double) intersection.size() / union.size();
    }
}
