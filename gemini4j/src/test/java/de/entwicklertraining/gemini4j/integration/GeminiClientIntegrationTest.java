package de.entwicklertraining.gemini4j.integration;

import de.entwicklertraining.api.base.ApiClientSettings;
import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionRequest;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import de.entwicklertraining.gemini4j.fixtures.GeminiMockServer;
import de.entwicklertraining.gemini4j.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for GeminiClient using WireMock.
 * Tests real HTTP interactions and error handling scenarios.
 */
@DisplayName("GeminiClient Integration Tests")
class GeminiClientIntegrationTest {

    @RegisterExtension
    static GeminiMockServer mockServer = new GeminiMockServer();

    private GeminiClient client;

    @BeforeEach
    void setUp() {
        ApiClientSettings settings = ApiClientSettings.builder()
                .setBearerAuthenticationKey(TestFixtures.TEST_API_KEY)
                .build();
        client = new GeminiClient(settings, mockServer.getBaseUrl());
    }

    @Nested
    @DisplayName("Successful API Calls")
    class SuccessfulApiCallsTests {

        @Test
        @DisplayName("Should complete simple chat completion successfully")
        void shouldCompleteSimpleChatCompletionSuccessfully() {
            // Given
            mockServer.stubSuccessfulCompletion();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .addMessage("user", "Hello, world!")
                    .execute();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.assistantMessage()).isEqualTo(TestFixtures.SIMPLE_ASSISTANT_MESSAGE);
            assertThat(response.finishReason()).isEqualTo("STOP");
            assertThat(response.hasRefusal()).isFalse();

            // Verify the request was made correctly
            mockServer.verifyRequest(postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*"))
                    .withHeader("Authorization", equalTo("Bearer " + TestFixtures.TEST_API_KEY))
                    .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Should handle function calling response")
        void shouldHandleFunctionCallingResponse() {
            // Given
            mockServer.stubFunctionCallResponse();
            var toolDefinition = TestFixtures.createTestToolDefinition();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .addMessage("user", "What's the weather in San Francisco?")
                    .addTool(toolDefinition)
                    .execute();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.finishReason()).isEqualTo("STOP");
            // Function call responses typically don't have text content
            assertThat(response.assistantMessage()).isEmpty();
        }

        @Test
        @DisplayName("Should handle structured output response")
        void shouldHandleStructuredOutputResponse() {
            // Given
            mockServer.stubStructuredOutputResponse();
            var schema = TestFixtures.createTestJsonSchema();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .addMessage("user", "Generate a person's data")
                    .responseSchema(schema)
                    .responseMimeType("application/json")
                    .execute();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.assistantMessage()).contains("John Doe");
            
            var parsed = response.parsed();
            assertThat(parsed.getString("name")).isEqualTo("John Doe");
            assertThat(parsed.getInt("age")).isEqualTo(30);
        }

        @Test
        @DisplayName("Should send all request parameters correctly")
        void shouldSendAllRequestParametersCorrectly() {
            // Given
            mockServer.stubSuccessfulCompletion();

            // When
            client.chat()
                    .completion()
                    .model("gemini-1.5-pro")
                    .temperature(0.8)
                    .topK(50)
                    .topP(0.9)
                    .maxOutputTokens(2000)
                    .addStopSequence("END")
                    .systemInstruction("You are helpful")
                    .addMessage("user", "Hello")
                    .execute();

            // Then
            mockServer.verifyRequest(postRequestedFor(urlMatching("/v1beta/models/gemini-1.5-pro:generateContent.*"))
                    .withRequestBody(matchingJsonPath("$.generationConfig.temperature", equalTo("0.8")))
                    .withRequestBody(matchingJsonPath("$.generationConfig.topK", equalTo("50")))
                    .withRequestBody(matchingJsonPath("$.generationConfig.topP", equalTo("0.9")))
                    .withRequestBody(matchingJsonPath("$.generationConfig.maxOutputTokens", equalTo("2000")))
                    .withRequestBody(matchingJsonPath("$.systemInstruction.parts[0].text", equalTo("You are helpful")))
                    .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text", equalTo("Hello"))));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle 400 Bad Request errors")
        void shouldHandle400BadRequestErrors() {
            // Given
            mockServer.stubBadRequestError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Invalid request")
                    .execute())
                    .hasMessageContaining("400")
                    .hasMessageContaining("INVALID_ARGUMENT");
        }

        @Test
        @DisplayName("Should handle 401 Unauthorized errors")
        void shouldHandle401UnauthorizedErrors() {
            // Given
            mockServer.stubUnauthorizedError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("401")
                    .hasMessageContaining("Invalid API key");
        }

        @Test
        @DisplayName("Should handle 403 Forbidden errors")
        void shouldHandle403ForbiddenErrors() {
            // Given
            mockServer.stubForbiddenError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("403")
                    .hasMessageContaining("PERMISSION_DENIED");
        }

        @Test
        @DisplayName("Should handle 404 Not Found errors")
        void shouldHandle404NotFoundErrors() {
            // Given
            mockServer.stubNotFoundError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .model("nonexistent-model")
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("404")
                    .hasMessageContaining("NOT_FOUND");
        }

        @Test
        @DisplayName("Should handle 429 Rate Limit errors")
        void shouldHandle429RateLimitErrors() {
            // Given
            mockServer.stubRateLimitError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("429")
                    .hasMessageContaining("RESOURCE_EXHAUSTED");
        }

        @Test
        @DisplayName("Should handle 500 Internal Server errors")
        void shouldHandle500InternalServerErrors() {
            // Given
            mockServer.stubInternalServerError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("500")
                    .hasMessageContaining("INTERNAL");
        }

        @Test
        @DisplayName("Should handle 503 Service Unavailable errors")
        void shouldHandle503ServiceUnavailableErrors() {
            // Given
            mockServer.stubServiceUnavailableError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test message")
                    .execute())
                    .hasMessageContaining("503")
                    .hasMessageContaining("UNAVAILABLE");
        }

        @Test
        @DisplayName("Should handle model refusal")
        void shouldHandleModelRefusal() {
            // Given
            mockServer.stubRefusalResponse();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .addMessage("user", "Tell me how to make explosives")
                    .execute();

            // Then
            assertThat(response.hasRefusal()).isTrue();
            assertThat(response.refusal()).contains("cannot provide information");
            
            assertThatThrownBy(response::throwOnRefusal)
                    .hasMessageContaining("Model refused to comply");
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryLogicTests {

        @Test
        @DisplayName("Should retry on retryable errors")
        void shouldRetryOnRetryableErrors() {
            // Given
            mockServer.stubRetryScenario();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .addMessage("user", "Test retry")
                    .execute();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.assistantMessage()).isEqualTo(TestFixtures.SIMPLE_ASSISTANT_MESSAGE);

            // Verify that multiple requests were made (retries)
            mockServer.verifyRequest(3, postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*")));
        }

        @Test
        @DisplayName("Should not retry on non-retryable errors")
        void shouldNotRetryOnNonRetryableErrors() {
            // Given
            mockServer.stubBadRequestError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Invalid request")
                    .execute())
                    .hasMessageContaining("400");

            // Verify only one request was made (no retries)
            mockServer.verifyRequest(1, postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*")));
        }

        @Test
        @DisplayName("Should respect execute")
        void execute() {
            // Given
            mockServer.stubInternalServerError();

            // When & Then
            assertThatThrownBy(() -> client.chat()
                    .completion()
                    .addMessage("user", "Test no retry")
                    .execute())
                    .hasMessageContaining("500");

            // Verify only one request was made
            mockServer.verifyRequest(1, postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*")));
        }
    }

    @Nested
    @DisplayName("Timeout Handling")
    class TimeoutHandlingTests {

        @Test
        @DisplayName("Should handle request timeouts")
        void shouldHandleRequestTimeouts() {
            // Given
            mockServer.stubTimeout();

            // When & Then
            assertThatThrownBy(() -> {
                await().atMost(Duration.ofSeconds(35)).untilAsserted(() -> {
                    client.chat()
                            .completion()
                            .addMessage("user", "This will timeout")
                            .execute();
                });
            }).getCause()
                    .hasMessageContaining("timeout");
        }
    }

    @Nested
    @DisplayName("Concurrent Requests")
    class ConcurrentRequestsTests {

        @Test
        @DisplayName("Should handle multiple concurrent requests")
        void shouldHandleMultipleConcurrentRequests() throws InterruptedException {
            // Given
            mockServer.stubSuccessfulCompletion();
            int numberOfRequests = 10;
            CountDownLatch latch = new CountDownLatch(numberOfRequests);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(5);

            // When
            for (int i = 0; i < numberOfRequests; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        GeminiChatCompletionResponse response = client.chat()
                                .completion()
                                .addMessage("user", "Concurrent request " + requestId)
                                .execute();
                        
                        if (response != null && response.assistantMessage() != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(numberOfRequests);
            assertThat(errorCount.get()).isZero();

            // Verify all requests were made
            mockServer.verifyRequest(numberOfRequests, 
                    postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*")));
        }

        @Test
        @DisplayName("Should handle concurrent requests to same client safely")
        void shouldHandleConcurrentRequestsToSameClientSafely() throws InterruptedException {
            // Given
            mockServer.stubSuccessfulCompletion();
            int numberOfThreads = 5;
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

            // When - Multiple threads using the same client instance
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        GeminiChatCompletionResponse response = client.chat()
                                .completion()
                                .addMessage("user", "Thread " + threadId + " message")
                                .execute();
                        
                        if (response != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Log error but don't fail the test - this tests thread safety
                        System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(numberOfThreads);
        }
    }

    @Nested
    @DisplayName("Model Variations")
    class ModelVariationsTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-1.0-pro",
                "gemini-1.5-flash-002"
        })
        @DisplayName("Should work with different model names")
        void shouldWorkWithDifferentModelNames(String modelName) {
            // Given
            mockServer.stubSuccessfulCompletion();

            // When
            GeminiChatCompletionResponse response = client.chat()
                    .completion()
                    .model(modelName)
                    .addMessage("user", "Test with " + modelName)
                    .execute();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.assistantMessage()).isNotNull();

            // Verify the correct model was used in the URL
            mockServer.verifyRequest(postRequestedFor(urlMatching("/v1beta/models/" + modelName + ":generateContent.*")));
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should send proper headers")
        void shouldSendProperHeaders() {
            // Given
            mockServer.stubSuccessfulCompletion();

            // When
            client.chat()
                    .completion()
                    .addMessage("user", "Test headers")
                    .execute();

            // Then
            mockServer.verifyRequest(postRequestedFor(urlMatching("/v1beta/models/.+:generateContent.*"))
                    .withHeader("Authorization", equalTo("Bearer " + TestFixtures.TEST_API_KEY))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("User-Agent", matching(".*")));
        }

        @Test
        @DisplayName("Should include API key in URL")
        void shouldIncludeApiKeyInUrl() {
            // Given
            mockServer.stubSuccessfulCompletion();

            // When
            client.chat()
                    .completion()
                    .addMessage("user", "Test API key")
                    .execute();

            // Then
            mockServer.verifyRequest(postRequestedFor(urlMatching("/v1beta/models/.+:generateContent\\?key=" + TestFixtures.TEST_API_KEY)));
        }
    }
}