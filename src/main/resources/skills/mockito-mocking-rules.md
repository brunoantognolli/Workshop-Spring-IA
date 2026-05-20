# Mockito 5 Mocking Rules & Guidelines

This guide details the strict rules and standards for utilizing Mockito 5 in combination with JUnit 5 in modern Spring Boot 3.3.x and Java 21 projects.

## 1. Extension Integration
Always boot JUnit 5 tests with the Mockito JUnit Jupiter extension. Do not use legacy runner annotations or manual mock lifecycle management unless strictly required.

```java
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    // Mocks will be automatically initialized and verified
}
```

## 2. Annotations: `@Mock` and `@InjectMocks`
*   Use `@Mock` to define mock instances of external collaborator interfaces or classes.
*   Use `@InjectMocks` on the class under test. Mockito will automatically inject all fields annotated with `@Mock` into this instance (via constructor, setter, or field injection).

```java
@Mock
private ProductRepository repository;

@Mock
private NotificationService notificationService;

@InjectMocks
private ProductService productService; // Under test; receives repository and notificationService
```

## 3. What NOT to Mock
*   **DTOs / Value Objects / Records:** Never mock plain Java objects, records, DTOs, or domain entities. Instantiate real objects instead (e.g., `new Order(1L, ...)`).
*   **Utility classes:** Do not mock utility classes with static methods unless absolutely necessary.
*   **Collections:** Use real lists, maps, or sets instead of mocking `java.util.List`.

## 4. Stubbing (Behavior Definition)
*   Use `when(...).thenReturn(...)` for standard stubbing.
*   Use `when(...).thenThrow(...)` to test exception flows.
*   Use `doReturn(...).when(...)` when dealing with spies, or to avoid side-effects during stubbing.

```java
// Standard stubbing
when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct));

// Throwing exception
when(repository.save(any(Product.class))).thenThrow(new DataAccessException("DB Offline") {});
```

### Strict Stubbing (Mockito 5 Default)
Mockito 5 applies strict stubbing by default. Stubbing a method that is never invoked during test execution causes an `UnnecessaryStubbingException` to fail the test.
*   Ensure all stubbed behaviors are actually executed.
*   If a mock stub is only used in some conditions or is shared, use `lenient()` or define stubbing locally within the nested test:
    ```java
    lenient().when(repository.findById(1L)).thenReturn(Optional.of(product));
    ```

## 5. Interaction Verification
*   Use `verify` to confirm that collaborators were called with the correct parameters.
*   Use `verifyNoInteractions(mock)` or `verifyNoMoreInteractions(mock)` to ensure no unexpected calls were made.

```java
// Verify exact number of calls
verify(notificationService, times(1)).sendEmail(anyString(), anyString());

// Verify no interaction occurred
verifyNoInteractions(paymentGateway);
```

## 6. Capturing Arguments
Use `@Captor` and `ArgumentCaptor<T>` to capture arguments passed to mocks for deep assertions.

```java
@Captor
private ArgumentCaptor<EmailRequest> emailCaptor;

@Test
void shouldSendValidEmail() {
    service.completeRegistration(user);
    
    verify(notificationService).sendEmail(emailCaptor.capture());
    EmailRequest captured = emailCaptor.getValue();
    
    assertThat(captured.getRecipient()).isEqualTo(user.getEmail());
    assertThat(captured.getSubject()).contains("Welcome");
}
```
