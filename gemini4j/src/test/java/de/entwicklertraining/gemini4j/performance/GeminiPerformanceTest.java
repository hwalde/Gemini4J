package de.entwicklertraining.gemini4j.performance;

import de.entwicklertraining.api.base.ApiClientSettings;
import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionRequest;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import de.entwicklertraining.gemini4j.fixtures.GeminiMockServer;
import de.entwicklertraining.gemini4j.fixtures.TestFixtures;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for Gemini4J library.
 * Tests throughput, latency, memory usage, and concurrent performance.
 * 
 * Run with: mvn test -Dperformance.tests.enabled=true
 */
@DisplayName("Gemini Performance Tests")
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
class GeminiPerformanceTest {

    @RegisterExtension
    static GeminiMockServer mockServer = new GeminiMockServer();

    private GeminiClient client;
    private static final int WARMUP_ITERATIONS = 10;
    private static final int PERFORMANCE_ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        ApiClientSettings settings = ApiClientSettings.builder()
                .setBearerAuthenticationKey(TestFixtures.TEST_API_KEY)
                .build();
        client = new GeminiClient(settings, mockServer.getBaseUrl());
        
        // Ensure fast responses for performance testing
        mockServer.stubSuccessfulCompletion();
    }

    @Nested
    @DisplayName("Request Building Performance")
    class RequestBuildingPerformanceTests {

        @Test
        @DisplayName("Should build simple requests efficiently")
        void shouldBuildSimpleRequestsEfficiently() {
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                client.chat().completion()
                        .addMessage("user", "test")
                        .build();
            }

            // Measure
            Instant start = Instant.now();
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                GeminiChatCompletionRequest request = client.chat().completion()
                        .addMessage("user", "Performance test message " + i)
                        .build();
                assertThat(request).isNotNull();
            }
            Duration elapsed = Duration.between(start, Instant.now());

            // Assert performance expectations
            long avgTimePerRequest = elapsed.toNanos() / PERFORMANCE_ITERATIONS;
            System.out.printf("Simple request building: %d requests in %d ms (avg: %.2f μs per request)%n",
                    PERFORMANCE_ITERATIONS, elapsed.toMillis(), avgTimePerRequest / 1000.0);

            assertThat(avgTimePerRequest).isLessThan(100_000); // Less than 100 microseconds per request
        }

        @Test
        @DisplayName("Should build complex requests efficiently")
        void shouldBuildComplexRequestsEfficiently() {
            var toolDefinition = TestFixtures.createTestToolDefinition();
            var safetySetting = TestFixtures.createTestSafetySetting();
            var schema = TestFixtures.createTestJsonSchema();

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                client.chat().completion()
                        .model("gemini-1.5-pro")
                        .temperature(0.8)
                        .topK(50)
                        .topP(0.9)
                        .maxOutputTokens(2000)
                        .systemInstruction("Complex system instruction")
                        .addMessage("user", "test")
                        .addTool(toolDefinition)
                        .safetySettings(List.of(safetySetting))
                        .responseSchema(schema)
                        .responseMimeType("application/json")
                        .thinking(1000)
                        .build();
            }

            // Measure
            Instant start = Instant.now();
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                GeminiChatCompletionRequest request = client.chat().completion()
                        .model("gemini-1.5-pro")
                        .temperature(0.8)
                        .topK(50)
                        .topP(0.9)
                        .maxOutputTokens(2000)
                        .systemInstruction("Complex system instruction " + i)
                        .addMessage("user", "Complex performance test message " + i)
                        .addTool(toolDefinition)
                        .safetySettings(List.of(safetySetting))
                        .responseSchema(schema)
                        .responseMimeType("application/json")
                        .thinking(1000)
                        .build();
                assertThat(request).isNotNull();
            }
            Duration elapsed = Duration.between(start, Instant.now());

            long avgTimePerRequest = elapsed.toNanos() / PERFORMANCE_ITERATIONS;
            System.out.printf("Complex request building: %d requests in %d ms (avg: %.2f μs per request)%n",
                    PERFORMANCE_ITERATIONS, elapsed.toMillis(), avgTimePerRequest / 1000.0);

            assertThat(avgTimePerRequest).isLessThan(500_000); // Less than 500 microseconds per request
        }

        @Test
        @DisplayName("Should handle large message lists efficiently")
        void shouldHandleLargeMessageListsEfficiently() {
            List<JSONObject> largeMessageList = TestFixtures.createLargeMessageList(1000);

            // Warmup
            for (int i = 0; i < 5; i++) {
                client.chat().completion()
                        .addAllMessages(largeMessageList)
                        .build();
            }

            // Measure
            Instant start = Instant.now();
            for (int i = 0; i < 10; i++) {
                GeminiChatCompletionRequest request = client.chat().completion()
                        .addAllMessages(largeMessageList)
                        .build();
                assertThat(request.messages()).hasSize(1000);
            }
            Duration elapsed = Duration.between(start, Instant.now());

            System.out.printf("Large message list handling: 10 requests with 1000 messages each in %d ms%n",
                    elapsed.toMillis());

            assertThat(elapsed.toMillis()).isLessThan(1000); // Less than 1 second for 10 requests
        }
    }

    @Nested
    @DisplayName("JSON Serialization Performance")
    class JsonSerializationPerformanceTests {

        @Test
        @DisplayName("Should serialize requests to JSON efficiently")
        void shouldSerializeRequestsToJsonEfficiently() {
            GeminiChatCompletionRequest request = client.chat().completion()
                    .addMessage("user", "Test message")
                    .temperature(0.7)
                    .build();

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                request.getBody();
            }

            // Measure
            Instant start = Instant.now();
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                String json = request.getBody();
                assertThat(json).isNotNull();
            }
            Duration elapsed = Duration.between(start, Instant.now());

            long avgTimePerSerialization = elapsed.toNanos() / PERFORMANCE_ITERATIONS;
            System.out.printf("JSON serialization: %d serializations in %d ms (avg: %.2f μs per serialization)%n",
                    PERFORMANCE_ITERATIONS, elapsed.toMillis(), avgTimePerSerialization / 1000.0);

            assertThat(avgTimePerSerialization).isLessThan(50_000); // Less than 50 microseconds per serialization
        }

        @Test
        @DisplayName("Should parse responses from JSON efficiently")
        void shouldParseResponsesFromJsonEfficiently() {
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            GeminiChatCompletionRequest mockRequest = client.chat().completion()
                    .addMessage("user", "test")
                    .build();

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                new GeminiChatCompletionResponse(new JSONObject(responseJson), mockRequest);
            }

            // Measure
            Instant start = Instant.now();
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(
                        new JSONObject(responseJson), mockRequest);
                assertThat(response.assistantMessage()).isNotNull();
            }
            Duration elapsed = Duration.between(start, Instant.now());

            long avgTimePerParsing = elapsed.toNanos() / PERFORMANCE_ITERATIONS;
            System.out.printf("JSON parsing: %d parsings in %d ms (avg: %.2f μs per parsing)%n",
                    PERFORMANCE_ITERATIONS, elapsed.toMillis(), avgTimePerParsing / 1000.0);

            assertThat(avgTimePerParsing).isLessThan(100_000); // Less than 100 microseconds per parsing
        }
    }

    @Nested
    @DisplayName("Concurrent Performance")
    class ConcurrentPerformanceTests {

        @Test
        @DisplayName("Should handle high concurrent load")
        void shouldHandleHighConcurrentLoad() throws InterruptedException {
            int numberOfThreads = 50;
            int requestsPerThread = 20;
            int totalRequests = numberOfThreads * requestsPerThread;
            
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);
            
            List<Future<List<Long>>> futures = new ArrayList<>();

            // Submit all tasks
            for (int t = 0; t < numberOfThreads; t++) {
                final int threadId = t;
                Future<List<Long>> future = executor.submit(() -> {
                    List<Long> responseTimes = new ArrayList<>();
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int r = 0; r < requestsPerThread; r++) {
                            Instant start = Instant.now();
                            
                            GeminiChatCompletionResponse response = client.chat()
                                    .completion()
                                    .addMessage("user", "Concurrent test " + threadId + "-" + r)
                                    .execute();
                            
                            long responseTime = Duration.between(start, Instant.now()).toMillis();
                            responseTimes.add(responseTime);
                            
                            assertThat(response).isNotNull();
                            assertThat(response.assistantMessage()).isNotNull();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Thread " + threadId + " failed", e);
                    } finally {
                        finishLatch.countDown();
                    }
                    return responseTimes;
                });
                futures.add(future);
            }

            // Start all threads simultaneously
            Instant overallStart = Instant.now();
            startLatch.countDown();
            
            // Wait for completion
            boolean completed = finishLatch.await(60, TimeUnit.SECONDS);
            Duration overallDuration = Duration.between(overallStart, Instant.now());
            
            executor.shutdown();
            assertThat(completed).isTrue();

            // Collect and analyze results
            List<Long> allResponseTimes = new ArrayList<>();
            for (Future<List<Long>> future : futures) {
                try {
                    allResponseTimes.addAll(future.get());
                } catch (ExecutionException e) {
                    fail("Thread execution failed", e.getCause());
                }
            }

            assertThat(allResponseTimes).hasSize(totalRequests);

            // Calculate statistics
            double avgResponseTime = allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxResponseTime = allResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            long minResponseTime = allResponseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            double throughput = (double) totalRequests / overallDuration.toMillis() * 1000;

            System.out.printf("Concurrent performance: %d requests across %d threads%n", totalRequests, numberOfThreads);
            System.out.printf("  Overall time: %d ms%n", overallDuration.toMillis());
            System.out.printf("  Throughput: %.2f requests/second%n", throughput);
            System.out.printf("  Response times - Min: %d ms, Avg: %.2f ms, Max: %d ms%n", 
                    minResponseTime, avgResponseTime, maxResponseTime);

            // Performance assertions
            assertThat(throughput).isGreaterThan(10); // At least 10 requests per second
            assertThat(avgResponseTime).isLessThan(1000); // Average response time under 1 second
            assertThat(maxResponseTime).isLessThan(5000); // No response takes more than 5 seconds
        }

        @Test
        @DisplayName("Should scale linearly with thread count")
        void shouldScaleLinearlyWithThreadCount() throws InterruptedException {
            int[] threadCounts = {1, 2, 4, 8};
            int requestsPerThread = 10;
            
            for (int threadCount : threadCounts) {
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                CountDownLatch latch = new CountDownLatch(threadCount);
                
                Instant start = Instant.now();
                
                for (int t = 0; t < threadCount; t++) {
                    executor.submit(() -> {
                        try {
                            for (int r = 0; r < requestsPerThread; r++) {
                                client.chat()
                                        .completion()
                                        .addMessage("user", "Scaling test")
                                        .execute();
                            }
                        } catch (Exception e) {
                            fail("Request failed", e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                boolean completed = latch.await(30, TimeUnit.SECONDS);
                Duration elapsed = Duration.between(start, Instant.now());
                executor.shutdown();
                
                assertThat(completed).isTrue();
                
                double throughput = (double) (threadCount * requestsPerThread) / elapsed.toMillis() * 1000;
                System.out.printf("Scaling test - Threads: %d, Throughput: %.2f req/sec%n", 
                        threadCount, throughput);
                
                // Each additional thread should provide meaningful improvement
                // (This is a loose check since we're using a mock server)
                assertThat(throughput).isGreaterThan(threadCount * 2);
            }
        }
    }

    @Nested
    @DisplayName("Memory Performance")
    class MemoryPerformanceTests {

        @Test
        @DisplayName("Should not leak memory during repeated requests")
        void shouldNotLeakMemoryDuringRepeatedRequests() {
            Runtime runtime = Runtime.getRuntime();
            
            // Force garbage collection and get baseline
            System.gc();
            System.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Perform many requests
            for (int i = 0; i < 1000; i++) {
                GeminiChatCompletionResponse response = client.chat()
                        .completion()
                        .addMessage("user", "Memory test " + i)
                        .execute();
                
                // Use the response to prevent optimization
                assertThat(response.assistantMessage()).isNotNull();
                
                // Occasional garbage collection
                if (i % 100 == 0) {
                    System.gc();
                }
            }
            
            // Force garbage collection and measure final memory
            System.gc();
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            
            long memoryIncrease = finalMemory - initialMemory;
            double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);
            
            System.out.printf("Memory usage - Initial: %.2f MB, Final: %.2f MB, Increase: %.2f MB%n",
                    initialMemory / (1024.0 * 1024.0),
                    finalMemory / (1024.0 * 1024.0),
                    memoryIncreaseMB);
            
            // Memory increase should be reasonable (less than 50MB for 1000 requests)
            assertThat(memoryIncreaseMB).isLessThan(50.0);
        }

        @Test
        @DisplayName("Should handle large request objects efficiently")
        void shouldHandleLargeRequestObjectsEfficiently() {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Create requests with large message lists
            List<GeminiChatCompletionRequest> requests = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                List<JSONObject> largeMessageList = TestFixtures.createLargeMessageList(100);
                GeminiChatCompletionRequest request = client.chat()
                        .completion()
                        .addAllMessages(largeMessageList)
                        .build();
                requests.add(request);
            }
            
            // Measure peak memory usage
            System.gc();
            long peakMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Clear references and force GC
            requests.clear();
            System.gc();
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            
            double peakIncreaseMB = (peakMemory - initialMemory) / (1024.0 * 1024.0);
            double finalIncreaseMB = (finalMemory - initialMemory) / (1024.0 * 1024.0);
            
            System.out.printf("Large objects memory - Peak increase: %.2f MB, Final increase: %.2f MB%n",
                    peakIncreaseMB, finalIncreaseMB);
            
            // Memory should be properly released after clearing references
            assertThat(finalIncreaseMB).isLessThan(peakIncreaseMB / 2);
        }
    }
}