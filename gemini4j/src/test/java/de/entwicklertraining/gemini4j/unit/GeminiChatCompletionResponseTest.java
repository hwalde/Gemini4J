package de.entwicklertraining.gemini4j.unit;

import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionRequest;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import de.entwicklertraining.gemini4j.fixtures.TestFixtures;
import de.entwicklertraining.api.base.ApiClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GeminiChatCompletionResponse.
 * Tests response parsing, content extraction, and error handling.
 */
@DisplayName("GeminiChatCompletionResponse Unit Tests")
class GeminiChatCompletionResponseTest {

    private GeminiChatCompletionRequest mockRequest;

    @BeforeEach
    void setUp() {
        var client = TestFixtures.createTestClient();
        mockRequest = client.chat().completion()
                .addMessage("user", "test")
                .build();
    }

    @Nested
    @DisplayName("Successful Response Parsing")
    class SuccessfulResponseParsingTests {

        @Test
        @DisplayName("Should parse successful response JSON")
        void shouldParseSuccessfulResponseJson() {
            // Given
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(responseJson);

            // When
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getJson()).isEqualTo(json);
        }

        @Test
        @DisplayName("Should extract assistant message from response")
        void shouldExtractAssistantMessageFromResponse() {
            // Given
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String assistantMessage = response.assistantMessage();

            // Then
            assertThat(assistantMessage).isEqualTo(TestFixtures.SIMPLE_ASSISTANT_MESSAGE);
        }

        @Test
        @DisplayName("Should extract finish reason from response")
        void shouldExtractFinishReasonFromResponse() {
            // Given
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String finishReason = response.finishReason();

            // Then
            assertThat(finishReason).isEqualTo("STOP");
        }

        @Test
        @DisplayName("Should indicate no refusal for successful response")
        void shouldIndicateNoRefusalForSuccessfulResponse() {
            // Given
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When & Then
            assertThat(response.hasRefusal()).isFalse();
            assertThat(response.refusal()).isNull();
        }

        @Test
        @DisplayName("Should not throw on refusal check for successful response")
        void shouldNotThrowOnRefusalCheckForSuccessfulResponse() {
            // Given
            String responseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When & Then
            assertThatCode(response::throwOnRefusal)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Function Call Response Parsing")
    class FunctionCallResponseParsingTests {

        @Test
        @DisplayName("Should handle function call response")
        void shouldHandleFunctionCallResponse() {
            // Given
            String responseJson = TestFixtures.createFunctionCallResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String assistantMessage = response.assistantMessage();

            // Then
            // Function call responses typically don't have text content
            assertThat(assistantMessage).isEmpty();
        }

        @Test
        @DisplayName("Should extract finish reason from function call response")
        void shouldExtractFinishReasonFromFunctionCallResponse() {
            // Given
            String responseJson = TestFixtures.createFunctionCallResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String finishReason = response.finishReason();

            // Then
            assertThat(finishReason).isEqualTo("STOP");
        }
    }

    @Nested
    @DisplayName("Structured Output Parsing")
    class StructuredOutputParsingTests {

        @Test
        @DisplayName("Should parse structured output response")
        void shouldParseStructuredOutputResponse() {
            // Given
            String responseJson = TestFixtures.createStructuredOutputResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String assistantMessage = response.assistantMessage();

            // Then
            assertThat(assistantMessage).isNotNull();
            assertThat(assistantMessage).contains("John Doe");
            assertThat(assistantMessage).contains("30");
        }

        @Test
        @DisplayName("Should parse JSON from assistant message")
        void shouldParseJsonFromAssistantMessage() {
            // Given
            String responseJson = TestFixtures.createStructuredOutputResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            JSONObject parsed = response.parsed();

            // Then
            assertThat(parsed).isNotNull();
            assertThat(parsed.getString("name")).isEqualTo("John Doe");
            assertThat(parsed.getInt("age")).isEqualTo(30);
        }

        @Test
        @DisplayName("Should convert to POJO")
        void shouldConvertToPojo() {
            // Given
            String responseJson = TestFixtures.createStructuredOutputResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            TestPerson person = response.convertTo(TestPerson.class);

            // Then
            assertThat(person).isNotNull();
            assertThat(person.getName()).isEqualTo("John Doe");
            assertThat(person.getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should throw on invalid JSON conversion")
        void shouldThrowOnInvalidJsonConversion() {
            // Given - Response with invalid JSON
            String invalidResponseJson = TestFixtures.createSuccessfulResponseJson();
            JSONObject json = new JSONObject(invalidResponseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When & Then
            assertThatThrownBy(() -> response.convertTo(TestPerson.class))
                    .isInstanceOf(ApiClient.ApiResponseUnusableException.class)
                    .hasMessageContaining("Failed to parse the model's JSON");
        }
    }

    @Nested
    @DisplayName("Refusal Response Handling")
    class RefusalResponseHandlingTests {

        @Test
        @DisplayName("Should detect refusal in response")
        void shouldDetectRefusalInResponse() {
            // Given
            String responseJson = TestFixtures.createRefusalResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When & Then
            assertThat(response.hasRefusal()).isTrue();
        }

        @Test
        @DisplayName("Should extract refusal message")
        void shouldExtractRefusalMessage() {
            // Given
            String responseJson = TestFixtures.createRefusalResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When
            String refusal = response.refusal();

            // Then
            assertThat(refusal).isEqualTo("I cannot provide information about that topic.");
        }

        @Test
        @DisplayName("Should throw exception on refusal when requested")
        void shouldThrowExceptionOnRefusalWhenRequested() {
            // Given
            String responseJson = TestFixtures.createRefusalResponseJson();
            JSONObject json = new JSONObject(responseJson);
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(json, mockRequest);

            // When & Then
            assertThatThrownBy(response::throwOnRefusal)
                    .isInstanceOf(ApiClient.ApiResponseUnusableException.class)
                    .hasMessageContaining("Model refused to comply");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            // Given
            JSONObject malformedJson = new JSONObject("{}");
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(malformedJson, mockRequest);

            // When & Then
            assertThat(response.assistantMessage()).isNull();
            assertThat(response.finishReason()).isNull();
            assertThat(response.hasRefusal()).isFalse();
            assertThat(response.refusal()).isNull();
        }

        @Test
        @DisplayName("Should handle missing candidates array")
        void shouldHandleMissingCandidatesArray() {
            // Given
            JSONObject incompleteJson = new JSONObject();
            incompleteJson.put("someOtherField", "value");
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(incompleteJson, mockRequest);

            // When & Then
            assertThat(response.assistantMessage()).isNull();
            assertThat(response.finishReason()).isNull();
            assertThat(response.hasRefusal()).isFalse();
        }

        @Test
        @DisplayName("Should handle empty candidates array")
        void shouldHandleEmptyCandidatesArray() {
            // Given
            JSONObject emptyJson = new JSONObject();
            emptyJson.put("candidates", new org.json.JSONArray());
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(emptyJson, mockRequest);

            // When & Then
            assertThat(response.assistantMessage()).isNull();
            assertThat(response.finishReason()).isNull();
            assertThat(response.hasRefusal()).isFalse();
        }

        @Test
        @DisplayName("Should handle null assistant message in parsed()")
        void shouldHandleNullAssistantMessageInParsed() {
            // Given
            JSONObject malformedJson = new JSONObject("{}");
            GeminiChatCompletionResponse response = new GeminiChatCompletionResponse(malformedJson, mockRequest);

            // When & Then
            assertThatThrownBy(response::parsed)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"STOP", "MAX_TOKENS", "SAFETY", "RECITATION", "OTHER"})
        @DisplayName("Should handle various finish reasons")
        void shouldHandleVariousFinishReasons(String finishReason) {
            // Given
            JSONObject response = new JSONObject();
            org.json.JSONArray candidates = new org.json.JSONArray();
            JSONObject candidate = new JSONObject();
            candidate.put("finishReason", finishReason);
            
            JSONObject content = new JSONObject();
            content.put("role", "model");
            org.json.JSONArray parts = new org.json.JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", "test response");
            parts.put(part);
            content.put("parts", parts);
            candidate.put("content", content);
            
            candidates.put(candidate);
            response.put("candidates", candidates);

            GeminiChatCompletionResponse geminiResponse = new GeminiChatCompletionResponse(response, mockRequest);

            // When
            String actualFinishReason = geminiResponse.finishReason();

            // Then
            assertThat(actualFinishReason).isEqualTo(finishReason);
        }

        @Test
        @DisplayName("Should handle response with multiple parts")
        void shouldHandleResponseWithMultipleParts() {
            // Given
            JSONObject response = new JSONObject();
            org.json.JSONArray candidates = new org.json.JSONArray();
            JSONObject candidate = new JSONObject();
            
            JSONObject content = new JSONObject();
            content.put("role", "model");
            org.json.JSONArray parts = new org.json.JSONArray();
            
            JSONObject part1 = new JSONObject();
            part1.put("text", "First part. ");
            parts.put(part1);
            
            JSONObject part2 = new JSONObject();
            part2.put("text", "Second part.");
            parts.put(part2);
            
            content.put("parts", parts);
            candidate.put("content", content);
            candidate.put("finishReason", "STOP");
            
            candidates.put(candidate);
            response.put("candidates", candidates);

            GeminiChatCompletionResponse geminiResponse = new GeminiChatCompletionResponse(response, mockRequest);

            // When
            String assistantMessage = geminiResponse.assistantMessage();

            // Then
            assertThat(assistantMessage).isEqualTo("First part. Second part.");
        }

        @Test
        @DisplayName("Should handle parts with mixed content types")
        void shouldHandlePartsWithMixedContentTypes() {
            // Given
            JSONObject response = new JSONObject();
            org.json.JSONArray candidates = new org.json.JSONArray();
            JSONObject candidate = new JSONObject();
            
            JSONObject content = new JSONObject();
            content.put("role", "model");
            org.json.JSONArray parts = new org.json.JSONArray();
            
            // Text part
            JSONObject textPart = new JSONObject();
            textPart.put("text", "Here's the result: ");
            parts.put(textPart);
            
            // Function call part (should be ignored for text extraction)
            JSONObject functionPart = new JSONObject();
            JSONObject functionCall = new JSONObject();
            functionCall.put("name", "some_function");
            functionPart.put("functionCall", functionCall);
            parts.put(functionPart);
            
            // Another text part
            JSONObject textPart2 = new JSONObject();
            textPart2.put("text", "Done!");
            parts.put(textPart2);
            
            content.put("parts", parts);
            candidate.put("content", content);
            candidate.put("finishReason", "STOP");
            
            candidates.put(candidate);
            response.put("candidates", candidates);

            GeminiChatCompletionResponse geminiResponse = new GeminiChatCompletionResponse(response, mockRequest);

            // When
            String assistantMessage = geminiResponse.assistantMessage();

            // Then
            assertThat(assistantMessage).isEqualTo("Here's the result: Done!");
        }
    }

    // Test POJO for conversion testing
    public static class TestPerson {
        private String name;
        private int age;

        public TestPerson() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}