package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

import java.nio.file.Path;

/**
 * Demonstrates how to use Vision with a local image (base64 encoded) in Gemini.
 * The image is located at "src/main/resources/images/AboutMe.jpg".
 * We pass it to the model and ask what is in the image.
 */
public class GeminiChatCompletionWithVisionBase64Example {

    public static void main(String[] args) {
        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Build a Gemini Completion request with an image in base64 format
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite") // or any Gemini model that supports vision
                .addMessage("user", "Please describe the following photo:")
                // Using the newly created method addImageByBase64
                .addImageByBase64(Path.of("gemini4j-examples", "src", "main", "resources", "image.jpg"))
                .execute();

        // Print model's response
        System.out.println("Model's answer:");
        System.out.println(response.assistantMessage());
    }
}