package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import org.json.JSONObject;

import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Demonstrates calling Gemini with an image (inline_data / base64).
 * Requires a vision-enabled model (e.g. gemini-2.5-flash-lite that supports images).
 */
public class GeminiChatCompletionWithVisionExample {

    public static void main(String[] args) throws Exception {
        // Suppose we have a local image "image_animal1.jpeg".
        // We'll base64 encode it and supply it as inline_data => "parts":[ {...}, {"inline_data": {...}} ]
        Path imagePath = Path.of("src", "main", "resources", "images", "AboutMe.jpg");
        byte[] fileBytes = Files.readAllBytes(imagePath);
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);

        // We'll construct a message with "inline_data"
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        // "parts" => array
        JSONObject textPart = new JSONObject().put("text", "Analyze this image's content, please.");
        JSONObject inlineDataPart = new JSONObject()
                .put("inline_data", new JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", base64Data)
                );

        userMsg.put("parts", List.of(textPart, inlineDataPart));

        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Build the request
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite")// "gemini-2.5-flash-lite") // vision-enabled
                .addAllMessages(List.of(userMsg))
                .execute();

        System.out.println("Vision analysis => " + response.assistantMessage());
    }
}
