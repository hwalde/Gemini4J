package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

/**
 * Demonstrates how to use Vision with an external URL in Gemini.
 * We pass an image URL and ask the model to describe what it sees.
 */
public class GeminiChatCompletionWithVisionUrlExample {

    public static void main(String[] args) {
        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Example: an external image URL
        String url = "https://software-quality-services.de/wp-content/uploads/2024/09/Walde_0141.jpg";

        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite") // or any Gemini model that supports vision
                .addMessage("user", "What do you see in this image?")
                // Using the newly created method addImageByUrl
                .addImageByUrl(url)
                .execute();

        System.out.println("Model's answer:");
        System.out.println(response.assistantMessage());
    }
}