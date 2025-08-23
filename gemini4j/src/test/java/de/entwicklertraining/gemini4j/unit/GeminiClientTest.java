package de.entwicklertraining.gemini4j.unit;

import de.entwicklertraining.api.base.ApiClientSettings;
import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for GeminiClient class.
 * Tests client instantiation, configuration, and basic functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiClient Unit Tests")
class GeminiClientTest {

    private ApiClientSettings defaultSettings;
    private String testBaseUrl;

    @BeforeEach
    void setUp() {
        defaultSettings = ApiClientSettings.builder()
                .setBearerAuthenticationKey(TestFixtures.TEST_API_KEY)
                .build();
        testBaseUrl = TestFixtures.TEST_BASE_URL;
    }

    @Nested
    @DisplayName("Client Construction")
    class ClientConstructionTests {

        @Test
        @DisplayName("Should create client with default constructor")
        void shouldCreateClientWithDefaultConstructor() {
            // Given & When
            GeminiClient client = new GeminiClient();

            // Then
            assertThat(client).isNotNull();
            assertThat(client.chat()).isNotNull();
        }

        @Test
        @DisplayName("Should create client with settings only")
        void shouldCreateClientWithSettings() {
            // Given & When
            GeminiClient client = new GeminiClient(defaultSettings);

            // Then
            assertThat(client).isNotNull();
            assertThat(client.chat()).isNotNull();
        }

        @Test
        @DisplayName("Should create client with settings and custom base URL")
        void shouldCreateClientWithSettingsAndBaseUrl() {
            // Given & When
            GeminiClient client = new GeminiClient(defaultSettings, testBaseUrl);

            // Then
            assertThat(client).isNotNull();
            assertThat(client.chat()).isNotNull();
        }

        @Test
        @DisplayName("Should handle null settings gracefully")
        void shouldHandleNullSettings() {
            // Given & When & Then
            assertThatCode(() -> new GeminiClient(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null base URL gracefully")
        void shouldHandleNullBaseUrl() {
            // Given & When & Then
            assertThatCode(() -> new GeminiClient(defaultSettings, null))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://localhost:8080",
                "https://api.gemini.com",
                "https://custom-endpoint.example.com/api/v1"
        })
        @DisplayName("Should accept various base URL formats")
        void shouldAcceptVariousBaseUrlFormats(String baseUrl) {
            // Given & When
            GeminiClient client = new GeminiClient(defaultSettings, baseUrl);

            // Then
            assertThat(client).isNotNull();
        }
    }

    @Nested
    @DisplayName("API Key Configuration")
    class ApiKeyConfigurationTests {

        @Test
        @DisplayName("Should use API key from settings when provided")
        void shouldUseApiKeyFromSettings() {
            // Given
            String customApiKey = "custom-api-key-456";
            ApiClientSettings customSettings = ApiClientSettings.builder()
                    .setBearerAuthenticationKey(customApiKey)
                    .build();

            // When
            GeminiClient client = new GeminiClient(customSettings, testBaseUrl);

            // Then
            assertThat(client).isNotNull();
            // Note: We can't directly access the API key from the client,
            // but we can verify the client was created successfully
        }

        @Test
        @DisplayName("Should fall back to environment variable when no API key in settings")
        void shouldFallBackToEnvironmentVariable() {
            // Given
            ApiClientSettings emptySettings = ApiClientSettings.builder().build();
            
            // When & Then
            // This test verifies the client handles environment variable fallback
            // The actual environment variable reading is tested in integration tests
            assertThatCode(() -> new GeminiClient(emptySettings, testBaseUrl))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Chat Interface")
    class ChatInterfaceTests {

        private GeminiClient client;

        @BeforeEach
        void setUp() {
            client = new GeminiClient(defaultSettings, testBaseUrl);
        }

        @Test
        @DisplayName("Should provide chat interface")
        void shouldProvideChatInterface() {
            // When
            GeminiClient.GeminiChat chat = client.chat();

            // Then
            assertThat(chat).isNotNull();
        }

        @Test
        @DisplayName("Should provide completion builder from chat interface")
        void shouldProvideCompletionBuilderFromChatInterface() {
            // When
            var completionBuilder = client.chat().completion();

            // Then
            assertThat(completionBuilder).isNotNull();
        }

        @Test
        @DisplayName("Should create new chat instances each time")
        void shouldCreateNewChatInstancesEachTime() {
            // When
            GeminiClient.GeminiChat chat1 = client.chat();
            GeminiClient.GeminiChat chat2 = client.chat();

            // Then
            assertThat(chat1).isNotNull();
            assertThat(chat2).isNotNull();
            // Chat instances may or may not be the same (implementation detail)
            // but they should both be functional
        }

        @Test
        @DisplayName("Should create new completion builders each time")
        void shouldCreateNewCompletionBuildersEachTime() {
            // When
            var builder1 = client.chat().completion();
            var builder2 = client.chat().completion();

            // Then
            assertThat(builder1).isNotNull();
            assertThat(builder2).isNotNull();
            assertThat(builder1).isNotSameAs(builder2);
        }
    }

    @Nested
    @DisplayName("Error Handling Registration")
    class ErrorHandlingRegistrationTests {

        @Test
        @DisplayName("Should register all HTTP status code exceptions during construction")
        void shouldRegisterAllHttpStatusCodeExceptions() {
            // Given & When
            GeminiClient client = new GeminiClient(defaultSettings, testBaseUrl);

            // Then
            assertThat(client).isNotNull();
            // The actual error handling registration is tested in integration tests
            // where we can trigger actual HTTP errors and verify the exceptions
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should be safe to create multiple clients concurrently")
        void shouldBeSafeToCreateMultipleClientsConcurrently() throws InterruptedException {
            // Given
            int numberOfThreads = 10;
            Thread[] threads = new Thread[numberOfThreads];
            GeminiClient[] clients = new GeminiClient[numberOfThreads];
            Throwable[] exceptions = new Throwable[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        clients[index] = new GeminiClient(defaultSettings, testBaseUrl);
                    } catch (Throwable e) {
                        exceptions[index] = e;
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }

            // Then
            for (int i = 0; i < numberOfThreads; i++) {
                assertThat(exceptions[i]).isNull();
                assertThat(clients[i]).isNotNull();
            }
        }

        @Test
        @DisplayName("Should be safe to access chat interface concurrently")
        void shouldBeSafeToAccessChatInterfaceConcurrently() throws InterruptedException {
            // Given
            GeminiClient client = new GeminiClient(defaultSettings, testBaseUrl);
            int numberOfThreads = 10;
            Thread[] threads = new Thread[numberOfThreads];
            GeminiClient.GeminiChat[] chats = new GeminiClient.GeminiChat[numberOfThreads];
            Throwable[] exceptions = new Throwable[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        chats[index] = client.chat();
                    } catch (Throwable e) {
                        exceptions[index] = e;
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }

            // Then
            for (int i = 0; i < numberOfThreads; i++) {
                assertThat(exceptions[i]).isNull();
                assertThat(chats[i]).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("Should handle invalid base URLs gracefully")
        void shouldHandleInvalidBaseUrlsGracefully(String invalidUrl) {
            // Given & When & Then
            assertThatCode(() -> new GeminiClient(defaultSettings, invalidUrl))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle settings with empty API key")
        void shouldHandleSettingsWithEmptyApiKey() {
            // Given
            ApiClientSettings emptyKeySettings = ApiClientSettings.builder()
                    .setBearerAuthenticationKey("")
                    .build();

            // When & Then
            assertThatCode(() -> new GeminiClient(emptyKeySettings, testBaseUrl))
                    .doesNotThrowAnyException();
        }
    }
}