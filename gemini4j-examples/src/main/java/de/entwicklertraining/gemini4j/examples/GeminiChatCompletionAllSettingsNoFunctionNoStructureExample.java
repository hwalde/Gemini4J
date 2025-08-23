package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import de.entwicklertraining.gemini4j.chat.completion.GeminiSafetySetting;

import java.util.List;

/**
 * Demonstrates a Gemini chat completion request that does NOT use:
 *  - structured outputs (no responseSchema)
 *  - function calling (no tools)
 * 
 * But uses:
 *  - temperature, topK, topP, maxOutputTokens
 *  - stop sequences
 *  - safety settings
 *  - parallelToolCalls
 *  - systemInstruction
 */
public class GeminiChatCompletionAllSettingsNoFunctionNoStructureExample {

    public static void main(String[] args) {
        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Build a request that uses (almost) all features except structured outputs & function calling
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite") // or "gemini-2.5-flash-lite", etc.
                .temperature(1.0)
                .topK(64)
                .topP(0.8)
                .maxOutputTokens(512)
                .addStopSequence("END_OF_TEXT")
                .safetySettings(List.of(
                        new GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
                        new GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH")
                ))
                .parallelToolCalls(false) // we'll keep it false here
                .responseMimeType("text/plain")
                .systemInstruction("You are a creative writing assistant. Follow the safety settings strictly.")
                // Add some user messages
                .addMessage("user", "Hello, I'd like a short adventurous story featuring a detective.")
                .execute();

        if (response.hasRefusal()) {
            System.out.println("The model refused: " + response.refusal());
        } else {
            System.out.println("Gemini Response:\n" + response.assistantMessage());
        }
    }
}
