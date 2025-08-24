package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

/**
 * A basic example of calling the Gemini chat completion to just generate text from text-only input.
 */
public class GeminiChatCompletionExample {

    public static void main(String[] args) {
        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Minimal usage:
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .addMessage("user", "Hello, how are you?")
                .execute();

        System.out.println("Gemini says: " + response.assistantMessage());
    }
}
