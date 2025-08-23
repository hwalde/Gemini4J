package de.entwicklertraining.gemini4j.fixtures;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock server for testing Gemini API interactions.
 * Provides a JUnit 5 extension that automatically starts/stops the mock server.
 */
public class GeminiMockServer implements BeforeEachCallback, AfterEachCallback {

    private WireMockServer wireMockServer;
    private int port;

    public GeminiMockServer() {
        this(0); // Use dynamic port
    }

    public GeminiMockServer(int port) {
        this.port = port;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        WireMockConfiguration config = WireMockConfiguration.options();
        if (port > 0) {
            config.port(port);
        } else {
            config.dynamicPort();
        }
        
        wireMockServer = new WireMockServer(config);
        wireMockServer.start();
        this.port = wireMockServer.port();
        
        // Configure the static WireMock instance
        WireMock.configureFor("localhost", this.port);
        
        setupDefaultStubs();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    /**
     * Sets up default stub responses for common scenarios.
     */
    private void setupDefaultStubs() {
        // Default successful response
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createSuccessfulResponseJson())));
    }

    /**
     * Stubs a successful chat completion response.
     */
    public void stubSuccessfulCompletion() {
        stubSuccessfulCompletion(TestFixtures.createSuccessfulResponseJson());
    }

    /**
     * Stubs a successful chat completion response with custom body.
     */
    public void stubSuccessfulCompletion(String responseBody) {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs a function call response.
     */
    public void stubFunctionCallResponse() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createFunctionCallResponseJson())));
    }

    /**
     * Stubs a structured output response.
     */
    public void stubStructuredOutputResponse() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createStructuredOutputResponseJson())));
    }

    /**
     * Stubs a refusal response.
     */
    public void stubRefusalResponse() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createRefusalResponseJson())));
    }

    /**
     * Stubs a 400 Bad Request error.
     */
    public void stubBadRequestError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createErrorResponseJson())));
    }

    /**
     * Stubs a 401 Unauthorized error.
     */
    public void stubUnauthorizedError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":401,\"message\":\"Invalid API key\",\"status\":\"UNAUTHENTICATED\"}}")));
    }

    /**
     * Stubs a 403 Forbidden error.
     */
    public void stubForbiddenError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":403,\"message\":\"Access denied\",\"status\":\"PERMISSION_DENIED\"}}")));
    }

    /**
     * Stubs a 404 Not Found error.
     */
    public void stubNotFoundError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":404,\"message\":\"Model not found\",\"status\":\"NOT_FOUND\"}}")));
    }

    /**
     * Stubs a 429 Rate Limit error.
     */
    public void stubRateLimitError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "60")
                        .withBody(TestFixtures.createRateLimitErrorJson())));
    }

    /**
     * Stubs a 500 Internal Server Error.
     */
    public void stubInternalServerError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":500,\"message\":\"Internal server error\",\"status\":\"INTERNAL\"}}")));
    }

    /**
     * Stubs a 503 Service Unavailable error.
     */
    public void stubServiceUnavailableError() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "120")
                        .withBody("{\"error\":{\"code\":503,\"message\":\"Service unavailable\",\"status\":\"UNAVAILABLE\"}}")));
    }

    /**
     * Stubs a timeout scenario.
     */
    public void stubTimeout() {
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .willReturn(aResponse()
                        .withFixedDelay(30000) // 30 second delay
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createSuccessfulResponseJson())));
    }

    /**
     * Stubs a scenario with multiple retries before success.
     */
    public void stubRetryScenario() {
        // First two calls fail with 500
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":500,\"message\":\"Temporary error\",\"status\":\"INTERNAL\"}}"))
                .willSetStateTo("First Retry"));

        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":500,\"message\":\"Temporary error\",\"status\":\"INTERNAL\"}}"))
                .willSetStateTo("Second Retry"));

        // Third call succeeds
        stubFor(post(urlMatching("/v1beta/models/.+:generateContent.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.createSuccessfulResponseJson())));
    }

    /**
     * Resets all stubs and request logs.
     */
    public void reset() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
            setupDefaultStubs();
        }
    }

    /**
     * Verifies that a request was made matching the given pattern.
     */
    public void verifyRequest(RequestPatternBuilder pattern) {
        verify(pattern);
    }

    /**
     * Verifies that exactly n requests were made matching the pattern.
     */
    public void verifyRequest(int count, RequestPatternBuilder pattern) {
        verify(count, pattern);
    }

    /**
     * Returns the base URL for the mock server.
     */
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Returns the port the mock server is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the WireMockServer instance for advanced usage.
     */
    public WireMockServer getWireMockServer() {
        return wireMockServer;
    }
}