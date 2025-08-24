package de.entwicklertraining.gemini4j.unit;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionRequest;
import de.entwicklertraining.gemini4j.fixtures.TestFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GeminiChatCompletionRequest and its Builder.
 * Tests request building, parameter validation, and JSON generation.
 */
@DisplayName("GeminiChatCompletionRequest Unit Tests")
class GeminiChatCompletionRequestTest {

    private GeminiClient client;
    private GeminiChatCompletionRequest.Builder builder;

    @BeforeEach
    void setUp() {
        client = TestFixtures.createTestClient();
        builder = client.chat().completion();
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create builder with default values")
        void shouldCreateBuilderWithDefaultValues() {
            // When
            GeminiChatCompletionRequest request = builder.build();

            // Then
            assertThat(request.model()).isEqualTo("gemini-1.5-flash");
            assertThat(request.temperature()).isNull();
            assertThat(request.topK()).isNull();
            assertThat(request.topP()).isNull();
            assertThat(request.maxOutputTokens()).isNull();
            assertThat(request.messages()).isEmpty();
            assertThat(request.tools()).isEmpty();
            assertThat(request.safetySettings()).isEmpty();
            assertThat(request.systemInstruction()).isNull();
            assertThat(request.thinkingBudget()).isNull();
        }

        @Test
        @DisplayName("Should build request with custom model")
        void shouldBuildRequestWithCustomModel() {
            // Given
            String customModel = "gemini-1.5-pro";

            // When
            GeminiChatCompletionRequest request = builder
                    .model(customModel)
                    .build();

            // Then
            assertThat(request.model()).isEqualTo(customModel);
        }

        @Test
        @DisplayName("Should build request with temperature")
        void shouldBuildRequestWithTemperature() {
            // Given
            Double temperature = 0.8;

            // When
            GeminiChatCompletionRequest request = builder
                    .temperature(temperature)
                    .build();

            // Then
            assertThat(request.temperature()).isEqualTo(temperature);
        }

        @Test
        @DisplayName("Should build request with generation parameters")
        void shouldBuildRequestWithGenerationParameters() {
            // Given
            Integer topK = 50;
            Double topP = 0.9;
            Integer maxTokens = 2000;

            // When
            GeminiChatCompletionRequest request = builder
                    .topK(topK)
                    .topP(topP)
                    .maxOutputTokens(maxTokens)
                    .build();

            // Then
            assertThat(request.topK()).isEqualTo(topK);
            assertThat(request.topP()).isEqualTo(topP);
            assertThat(request.maxOutputTokens()).isEqualTo(maxTokens);
        }

        @Test
        @DisplayName("Should build request with stop sequences")
        void shouldBuildRequestWithStopSequences() {
            // Given
            List<String> stopSequences = Arrays.asList("END", "STOP", "FINISH");

            // When
            GeminiChatCompletionRequest request = builder
                    .stopSequences(stopSequences)
                    .build();

            // Then
            assertThat(request.stopSequences()).containsExactlyElementsOf(stopSequences);
        }

        @Test
        @DisplayName("Should add individual stop sequence")
        void shouldAddIndividualStopSequence() {
            // When
            GeminiChatCompletionRequest request = builder
                    .addStopSequence("END")
                    .addStopSequence("STOP")
                    .build();

            // Then
            assertThat(request.stopSequences()).containsExactly("END", "STOP");
        }

        @Test
        @DisplayName("Should build request with system instruction")
        void shouldBuildRequestWithSystemInstruction() {
            // Given
            String systemInstruction = "You are a helpful assistant.";

            // When
            GeminiChatCompletionRequest request = builder
                    .systemInstruction(systemInstruction)
                    .build();

            // Then
            assertThat(request.systemInstruction()).isEqualTo(systemInstruction);
        }

        @Test
        @DisplayName("Should build request with thinking budget")
        void shouldBuildRequestWithThinkingBudget() {
            // Given
            Integer thinkingBudget = 1000;

            // When
            GeminiChatCompletionRequest request = builder
                    .thinking(thinkingBudget)
                    .build();

            // Then
            assertThat(request.thinkingBudget()).isEqualTo(thinkingBudget);
        }
    }

    @Nested
    @DisplayName("Message Handling")
    class MessageHandlingTests {

        @Test
        @DisplayName("Should add simple text message")
        void shouldAddSimpleTextMessage() {
            // When
            GeminiChatCompletionRequest request = builder
                    .addMessage("user", "Hello, world!")
                    .build();

            // Then
            assertThat(request.messages()).hasSize(1);
            JSONObject message = request.messages().get(0);
            assertThat(message.getString("role")).isEqualTo("user");
            assertThat(message.getJSONArray("parts")).hasSize(1);
            assertThat(message.getJSONArray("parts").getJSONObject(0).getString("text"))
                    .isEqualTo("Hello, world!");
        }

        @Test
        @DisplayName("Should add multiple messages")
        void shouldAddMultipleMessages() {
            // When
            GeminiChatCompletionRequest request = builder
                    .addMessage("user", "Hello")
                    .addMessage("model", "Hi there!")
                    .addMessage("user", "How are you?")
                    .build();

            // Then
            assertThat(request.messages()).hasSize(3);
            assertThat(request.messages().get(0).getString("role")).isEqualTo("user");
            assertThat(request.messages().get(1).getString("role")).isEqualTo("model");
            assertThat(request.messages().get(2).getString("role")).isEqualTo("user");
        }

        @Test
        @DisplayName("Should add all messages from list")
        void shouldAddAllMessagesFromList() {
            // Given
            List<JSONObject> messages = Arrays.asList(
                    TestFixtures.createUserMessage("Message 1"),
                    TestFixtures.createAssistantMessage("Response 1"),
                    TestFixtures.createUserMessage("Message 2")
            );

            // When
            GeminiChatCompletionRequest request = builder
                    .addAllMessages(messages)
                    .build();

            // Then
            assertThat(request.messages()).hasSize(3);
            assertThat(request.messages()).containsExactlyElementsOf(messages);
        }
    }

    @Nested
    @DisplayName("Tools and Function Calling")
    class ToolsAndFunctionCallingTests {

        @Test
        @DisplayName("Should add tool definition")
        void shouldAddToolDefinition() {
            // Given
            var toolDefinition = TestFixtures.createTestToolDefinition();

            // When
            GeminiChatCompletionRequest request = builder
                    .addTool(toolDefinition)
                    .build();

            // Then
            assertThat(request.tools()).hasSize(1);
            assertThat(request.tools().get(0)).isEqualTo(toolDefinition);
        }

        @Test
        @DisplayName("Should add multiple tools")
        void shouldAddMultipleTools() {
            // Given
            var tool1 = TestFixtures.createTestToolDefinition();
            var tool2 = TestFixtures.createTestToolDefinition();
            var tools = Arrays.asList(tool1, tool2);

            // When
            GeminiChatCompletionRequest request = builder
                    .tools(tools)
                    .build();

            // Then
            assertThat(request.tools()).hasSize(2);
            assertThat(request.tools()).containsExactlyElementsOf(tools);
        }

        @Test
        @DisplayName("Should set parallel tool calls")
        void shouldSetParallelToolCalls() {
            // When
            GeminiChatCompletionRequest request = builder
                    .parallelToolCalls(true)
                    .build();

            // Then
            assertThat(request.parallelToolCalls()).isTrue();
        }
    }

    @Nested
    @DisplayName("Safety Settings")
    class SafetySettingsTests {

        @Test
        @DisplayName("Should add safety settings")
        void shouldAddSafetySettings() {
            // Given
            var safetySetting = TestFixtures.createTestSafetySetting();
            var safetySettings = Arrays.asList(safetySetting);

            // When
            GeminiChatCompletionRequest request = builder
                    .safetySettings(safetySettings)
                    .build();

            // Then
            assertThat(request.safetySettings()).hasSize(1);
            assertThat(request.safetySettings()).containsExactlyElementsOf(safetySettings);
        }
    }

    @Nested
    @DisplayName("Structured Output")
    class StructuredOutputTests {

        @Test
        @DisplayName("Should set response schema")
        void shouldSetResponseSchema() {
            // Given
            var schema = TestFixtures.createTestJsonSchema();

            // When
            GeminiChatCompletionRequest request = builder
                    .responseSchema(schema)
                    .build();

            // Then
            assertThat(request.responseSchema()).isEqualTo(schema);
        }

        @Test
        @DisplayName("Should set response MIME type")
        void shouldSetResponseMimeType() {
            // Given
            String mimeType = "application/json";

            // When
            GeminiChatCompletionRequest request = builder
                    .responseMimeType(mimeType)
                    .build();

            // Then
            assertThat(request.responseMimeType()).isEqualTo(mimeType);
        }
    }

    @Nested
    @DisplayName("Image Handling")
    class ImageHandlingTests {

        @Test
        @DisplayName("Should reject unsupported image extensions")
        void shouldRejectUnsupportedImageExtensions() {
            // Given
            String unsupportedUrl = "https://example.com/image.gif";

            // When & Then
            assertThatThrownBy(() -> builder.addImageByUrl(unsupportedUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported file extension");
        }

        @Test
        @DisplayName("Should accept supported image extensions")
        void shouldAcceptSupportedImageExtensions() {
            // Given - Use a real working URL
            String imageUrl = "https://software-quality-services.de/wp-content/uploads/2024/09/Walde_0141.jpg";
            
            // When & Then
            assertThatCode(() -> builder.addImageByUrl(imageUrl))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null image URL")
        void shouldHandleNullImageUrl() {
            // When & Then
            assertThatThrownBy(() -> builder.addImageByUrl(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null image path")
        void shouldHandleNullImagePath() {
            // When & Then
            assertThatThrownBy(() -> builder.addImageByBase64(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should reject unsupported file extension for local files")
        void shouldRejectUnsupportedFileExtensionForLocalFiles() {
            // Given
            var unsupportedPath = Paths.get("test.gif");

            // When & Then
            assertThatThrownBy(() -> builder.addImageByBase64(unsupportedPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported file extension");
        }
    }

    @Nested
    @DisplayName("JSON Generation")
    class JsonGenerationTests {

        @Test
        @DisplayName("Should generate valid JSON body")
        void shouldGenerateValidJsonBody() {
            // Given
            GeminiChatCompletionRequest request = builder
                    .addMessage("user", "Hello")
                    .temperature(0.7)
                    .build();

            // When
            String jsonBody = request.getBody();

            // Then
            assertThat(jsonBody).isNotNull();
            assertThatCode(() -> new JSONObject(jsonBody))
                    .doesNotThrowAnyException();

            JSONObject json = new JSONObject(jsonBody);
            assertThat(json.has("contents")).isTrue();
            assertThat(json.has("generationConfig")).isTrue();
        }

        @Test
        @DisplayName("Should include system instruction in JSON")
        void shouldIncludeSystemInstructionInJson() {
            // Given
            String systemInstruction = "You are helpful.";
            GeminiChatCompletionRequest request = builder
                    .systemInstruction(systemInstruction)
                    .addMessage("user", "Hello")
                    .build();

            // When
            String jsonBody = request.getBody();
            JSONObject json = new JSONObject(jsonBody);

            // Then
            assertThat(json.has("systemInstruction")).isTrue();
            JSONObject sysInst = json.getJSONObject("systemInstruction");
            assertThat(sysInst.getString("role")).isEqualTo("user");
            assertThat(sysInst.getJSONArray("parts").getJSONObject(0).getString("text"))
                    .isEqualTo(systemInstruction);
        }

        @Test
        @DisplayName("Should include thinking config in generation config")
        void shouldIncludeThinkingConfigInGenerationConfig() {
            // Given
            Integer thinkingBudget = 500;
            GeminiChatCompletionRequest request = builder
                    .thinking(thinkingBudget)
                    .addMessage("user", "Hello")
                    .build();

            // When
            String jsonBody = request.getBody();
            JSONObject json = new JSONObject(jsonBody);

            // Then
            assertThat(json.has("generationConfig")).isTrue();
            JSONObject genConfig = json.getJSONObject("generationConfig");
            assertThat(genConfig.has("thinkingConfig")).isTrue();
            assertThat(genConfig.getJSONObject("thinkingConfig").getInt("thinkingBudget"))
                    .isEqualTo(thinkingBudget);
        }

        @Test
        @DisplayName("Should include tools in correct format")
        void shouldIncludeToolsInCorrectFormat() {
            // Given
            var tool = TestFixtures.createTestToolDefinition();
            GeminiChatCompletionRequest request = builder
                    .addTool(tool)
                    .addMessage("user", "Hello")
                    .build();

            // When
            String jsonBody = request.getBody();
            JSONObject json = new JSONObject(jsonBody);

            // Then
            assertThat(json.has("tools")).isTrue();
            assertThat(json.has("toolConfig")).isTrue();

            JSONArray tools = json.getJSONArray("tools");
            assertThat(tools).hasSize(1);
            assertThat(tools.getJSONObject(0).has("functionDeclarations")).isTrue();

            JSONObject toolConfig = json.getJSONObject("toolConfig");
            assertThat(toolConfig.has("functionCallingConfig")).isTrue();
        }
    }

    @Nested
    @DisplayName("URL Generation")
    class UrlGenerationTests {

        @Test
        @DisplayName("Should generate correct relative URL with default model")
        void shouldGenerateCorrectRelativeUrlWithDefaultModel() {
            // Given
            GeminiChatCompletionRequest request = builder.build();

            // When
            String relativeUrl = request.getRelativeUrl();

            // Then
            assertThat(relativeUrl).isEqualTo("/v1beta/models/gemini-1.5-flash:generateContent?key=" + TestFixtures.TEST_API_KEY);
        }

        @Test
        @DisplayName("Should generate correct relative URL with custom model")
        void shouldGenerateCorrectRelativeUrlWithCustomModel() {
            // Given
            String customModel = "gemini-1.5-pro";
            GeminiChatCompletionRequest request = builder
                    .model(customModel)
                    .build();

            // When
            String relativeUrl = request.getRelativeUrl();

            // Then
            assertThat(relativeUrl).isEqualTo("/v1beta/models/" + customModel + ":generateContent?key=" + TestFixtures.TEST_API_KEY);
        }

        @Test
        @DisplayName("Should use POST HTTP method")
        void shouldUsePostHttpMethod() {
            // Given
            GeminiChatCompletionRequest request = builder.build();

            // When
            String method = request.getHttpMethod();

            // Then
            assertThat(method).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("Builder Validation")
    class BuilderValidationTests {

        @Test
        @DisplayName("Should handle null model gracefully")
        void shouldHandleNullModelGracefully() {
            // When & Then
            assertThatCode(() -> builder.model(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle negative temperature")
        void shouldHandleNegativeTemperature() {
            // When & Then
            assertThatCode(() -> builder.temperature(-0.5).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle temperature above 2.0")
        void shouldHandleTemperatureAbove2() {
            // When & Then
            assertThatCode(() -> builder.temperature(3.0).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle negative topK")
        void shouldHandleNegativeTopK() {
            // When & Then
            assertThatCode(() -> builder.topK(-10).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle topP outside valid range")
        void shouldHandleTopPOutsideValidRange() {
            // When & Then
            assertThatCode(() -> builder.topP(-0.5).build())
                    .doesNotThrowAnyException();
            assertThatCode(() -> builder.topP(1.5).build())
                    .doesNotThrowAnyException();
        }
    }
}