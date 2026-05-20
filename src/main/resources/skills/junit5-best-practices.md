# JUnit 5 Testing Guidelines & Best Practices

This guide describes the standard conventions and rules for writing unit tests using JUnit 5 (JUnit Jupiter) in modern Spring Boot 3.3.x and Java 21 applications.

## 1. Imports and Basic Annotations
Always use the modern JUnit Jupiter API. Do not use legacy JUnit 4 classes.

| Concept | JUnit 5 (Correct) | JUnit 4 (Forbidden) |
|---|---|---|
| Test annotation | `org.junit.jupiter.api.Test` | `org.junit.Test` |
| Before setup | `org.junit.jupiter.api.BeforeEach` | `org.junit.Before` |
| After cleanup | `org.junit.jupiter.api.AfterEach` | `org.junit.After` |
| Before all | `org.junit.jupiter.api.BeforeAll` | `org.junit.BeforeClass` |
| Ignore test | `org.junit.jupiter.api.Disabled` | `org.junit.Ignore` |
| Extension support | `@ExtendWith` | `@RunWith` |

## 2. Test Class Naming and Display Names
*   **Naming:** The test class name must end with `Test` (e.g., `OrderServiceTest` for `OrderService`).
*   **Test Intent:** Use `@DisplayName("...")` on both classes and methods to clearly explain the test scenario in human-readable English.
*   **Method Naming:** Use a standard, descriptive naming pattern:
    *   `should[Behavior]When[Condition]`
    *   Example: `shouldCreateOrderWhenDetailsAreValid()`
    *   Example: `shouldThrowExceptionWhenItemIsOutOfStock()`

## 3. Assertions (Prefer AssertJ)
Spring Boot 3.3.x imports AssertJ by default. Use AssertJ's fluent assertions `assertThat` from `org.assertj.core.api.Assertions.assertThat` instead of basic `assertEquals`.

```java
// AssertJ Fluent Assertions (RECOMMENDED)
assertThat(result).isNotNull();
assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
assertThat(result.getItems()).hasSize(3).contains(expectedItem);

// Multiple Assertions (JUnit 5 assertAll)
assertAll("Order attributes",
    () -> assertEquals(OrderStatus.PENDING, order.getStatus()),
    () -> assertEquals(100.0, order.getTotalPrice())
);
```

## 4. Exception Testing
To verify exceptions, use `org.junit.jupiter.api.Assertions.assertThrows()`. Do not use JUnit 4's `@Test(expected = ...)` or manual `try-catch` blocks.

```java
@Test
@DisplayName("Should throw IllegalArgumentException when price is negative")
void shouldThrowExceptionWhenPriceIsNegative() {
    OrderService service = new OrderService();
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.processOrder(-10.0)
    );
    
    assertThat(exception.getMessage()).contains("Price cannot be negative");
}
```

## 5. Grouping Tests with `@Nested`
Group related scenarios together inside a nested test class annotated with `@Nested` to improve readability and structure.

```java
class OrderServiceTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {
        @Test
        @DisplayName("Should create order successfully")
        void shouldCreate() { ... }
    }

    @Nested
    @DisplayName("Cancellation Tests")
    class CancellationTests {
        @Test
        @DisplayName("Should cancel order successfully")
        void shouldCancel() { ... }
    }
}
```
