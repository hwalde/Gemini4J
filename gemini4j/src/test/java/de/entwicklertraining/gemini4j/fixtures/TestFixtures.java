package de.entwicklertraining.gemini4j.fixtures;

import de.entwicklertraining.api.base.ApiClientSettings;
import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.GeminiJsonSchema;
import de.entwicklertraining.gemini4j.GeminiToolDefinition;
import de.entwicklertraining.gemini4j.GeminiToolResult;
import de.entwicklertraining.gemini4j.GeminiToolsCallback;
import de.entwicklertraining.gemini4j.chat.completion.GeminiSafetySetting;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Test fixtures and helper data for Gemini4J tests.
 * Provides consistent test data across all test classes.
 */
public class TestFixtures {

    // Test API Configuration
    public static final String TEST_API_KEY = "test-api-key-123";
    public static final String TEST_BASE_URL = "http://localhost:8080";
    public static final String DEFAULT_MODEL = "gemini-1.5-flash";

    // Test Messages
    public static final String SIMPLE_USER_MESSAGE = "Hello, how are you?";
    public static final String SIMPLE_ASSISTANT_MESSAGE = "I'm doing well, thank you for asking!";
    public static final String SYSTEM_INSTRUCTION = "You are a helpful AI assistant.";

    // Test Parameters
    public static final Double TEST_TEMPERATURE = 0.7;
    public static final Integer TEST_TOP_K = 40;
    public static final Double TEST_TOP_P = 0.95;
    public static final Integer TEST_MAX_OUTPUT_TOKENS = 1000;
    public static final List<String> TEST_STOP_SEQUENCES = Arrays.asList("END", "STOP");

    /**
     * Creates a basic test GeminiClient with default settings.
     */
    public static GeminiClient createTestClient() {
        ApiClientSettings settings = ApiClientSettings.builder()
                .setBearerAuthenticationKey(TEST_API_KEY)
                .build();
        return new GeminiClient(settings, TEST_BASE_URL);
    }

    /**
     * Creates a basic user message JSON object.
     */
    public static JSONObject createUserMessage(String content) {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", content));
        message.put("parts", parts);
        return message;
    }

    /**
     * Creates an assistant message JSON object.
     */
    public static JSONObject createAssistantMessage(String content) {
        JSONObject message = new JSONObject();
        message.put("role", "model");
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", content));
        message.put("parts", parts);
        return message;
    }

    /**
     * Creates a test safety setting.
     */
    public static GeminiSafetySetting createTestSafetySetting() {
        return new GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE");
    }

    /**
     * Creates a test tool definition for function calling.
     */
    public static GeminiToolDefinition createTestToolDefinition() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        
        JSONObject properties = new JSONObject();
        JSONObject locationProp = new JSONObject();
        locationProp.put("type", "string");
        locationProp.put("description", "The city and state");
        properties.put("location", locationProp);
        
        schema.put("properties", properties);
        schema.put("required", new JSONArray().put("location"));

        // Create a simple callback for testing
        GeminiToolsCallback callback = (context) -> GeminiToolResult.of(new JSONObject().put("result", "Weather data for location"));
        
        // Use the constructor directly with all required parameters
        return GeminiToolDefinition.builder("get_weather")
                .description("Get current weather for a location")
                .parameter("location", GeminiJsonSchema.stringSchema("The city and state"), true)
                .callback(callback)
                .build();
    }

    /**
     * Creates a test JSON schema for structured output.
     */
    public static GeminiJsonSchema createTestJsonSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        
        JSONObject properties = new JSONObject();
        JSONObject nameProp = new JSONObject();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        
        JSONObject ageProp = new JSONObject();
        ageProp.put("type", "integer");
        properties.put("age", ageProp);
        
        schema.put("properties", properties);
        schema.put("required", new JSONArray().put("name"));

        // Use the static factory method to create an object schema
        return GeminiJsonSchema.objectSchema()
                .property("name", GeminiJsonSchema.stringSchema("Person's name"), true)
                .property("age", GeminiJsonSchema.integerSchema("Person's age"), false);
    }

    /**
     * Sample successful API response JSON.
     */
    public static String createSuccessfulResponseJson() {
        JSONObject response = new JSONObject();
        
        JSONArray candidates = new JSONArray();
        JSONObject candidate = new JSONObject();
        
        JSONObject content = new JSONObject();
        content.put("role", "model");
        
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", SIMPLE_ASSISTANT_MESSAGE);
        parts.put(part);
        
        content.put("parts", parts);
        candidate.put("content", content);
        candidate.put("finishReason", "STOP");
        
        candidates.put(candidate);
        response.put("candidates", candidates);
        
        // Add usage metadata
        JSONObject usageMetadata = new JSONObject();
        usageMetadata.put("promptTokenCount", 10);
        usageMetadata.put("candidatesTokenCount", 15);
        usageMetadata.put("totalTokenCount", 25);
        response.put("usageMetadata", usageMetadata);
        
        return response.toString();
    }

    /**
     * Sample function call response JSON.
     */
    public static String createFunctionCallResponseJson() {
        JSONObject response = new JSONObject();
        
        JSONArray candidates = new JSONArray();
        JSONObject candidate = new JSONObject();
        
        JSONObject content = new JSONObject();
        content.put("role", "model");
        
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        
        JSONObject functionCall = new JSONObject();
        functionCall.put("name", "get_weather");
        JSONObject args = new JSONObject();
        args.put("location", "San Francisco, CA");
        functionCall.put("args", args);
        
        part.put("functionCall", functionCall);
        parts.put(part);
        
        content.put("parts", parts);
        candidate.put("content", content);
        candidate.put("finishReason", "STOP");
        
        candidates.put(candidate);
        response.put("candidates", candidates);
        
        return response.toString();
    }

    /**
     * Sample error response JSON for HTTP 400.
     */
    public static String createErrorResponseJson() {
        JSONObject error = new JSONObject();
        error.put("code", 400);
        error.put("message", "Invalid request: missing required field 'contents'");
        error.put("status", "INVALID_ARGUMENT");
        
        JSONObject response = new JSONObject();
        response.put("error", error);
        
        return response.toString();
    }

    /**
     * Sample rate limit error response JSON for HTTP 429.
     */
    public static String createRateLimitErrorJson() {
        JSONObject error = new JSONObject();
        error.put("code", 429);
        error.put("message", "Resource has been exhausted (e.g. check quota).");
        error.put("status", "RESOURCE_EXHAUSTED");
        
        JSONObject response = new JSONObject();
        response.put("error", error);
        
        return response.toString();
    }

    /**
     * Sample refusal response JSON.
     */
    public static String createRefusalResponseJson() {
        JSONObject response = new JSONObject();
        
        JSONArray candidates = new JSONArray();
        JSONObject candidate = new JSONObject();
        
        JSONObject content = new JSONObject();
        content.put("role", "model");
        content.put("refusal", "I cannot provide information about that topic.");
        
        JSONArray parts = new JSONArray();
        content.put("parts", parts);
        
        candidate.put("content", content);
        candidate.put("finishReason", "OTHER");
        
        candidates.put(candidate);
        response.put("candidates", candidates);
        
        return response.toString();
    }

    /**
     * Path to test image file.
     */
    public static Path getTestImagePath() {
        return Paths.get("src/test/resources/test-data/test-image.jpg");
    }

    /**
     * Base64 encoded test image data.
     */
    public static String getTestImageBase64() {
        return "/9j/4AAQSkZJRgABAQEAAAAAAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A8A";
    }

    /**
     * Sample structured output response.
     */
    public static String createStructuredOutputResponseJson() {
        JSONObject response = new JSONObject();
        
        JSONArray candidates = new JSONArray();
        JSONObject candidate = new JSONObject();
        
        JSONObject content = new JSONObject();
        content.put("role", "model");
        
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        
        // Structured JSON response
        JSONObject structuredData = new JSONObject();
        structuredData.put("name", "John Doe");
        structuredData.put("age", 30);
        
        part.put("text", structuredData.toString());
        parts.put(part);
        
        content.put("parts", parts);
        candidate.put("content", content);
        candidate.put("finishReason", "STOP");
        
        candidates.put(candidate);
        response.put("candidates", candidates);
        
        return response.toString();
    }

    /**
     * Creates a test request JSON body for validation.
     */
    public static String createTestRequestJson() {
        JSONObject request = new JSONObject();
        
        // System instruction
        JSONObject systemInstruction = new JSONObject();
        systemInstruction.put("role", "user");
        JSONArray sysParts = new JSONArray();
        sysParts.put(new JSONObject().put("text", SYSTEM_INSTRUCTION));
        systemInstruction.put("parts", sysParts);
        request.put("systemInstruction", systemInstruction);
        
        // Contents
        JSONArray contents = new JSONArray();
        contents.put(createUserMessage(SIMPLE_USER_MESSAGE));
        request.put("contents", contents);
        
        // Generation config
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", TEST_TEMPERATURE);
        generationConfig.put("topK", TEST_TOP_K);
        generationConfig.put("topP", TEST_TOP_P);
        generationConfig.put("maxOutputTokens", TEST_MAX_OUTPUT_TOKENS);
        request.put("generationConfig", generationConfig);
        
        return request.toString();
    }

    /**
     * Performance test data - large message array.
     */
    public static List<JSONObject> createLargeMessageList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createUserMessage("Test message " + i))
                .toList();
    }
}