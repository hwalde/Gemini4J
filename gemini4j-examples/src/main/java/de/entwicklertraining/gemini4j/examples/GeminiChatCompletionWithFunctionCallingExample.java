package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.*;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

import org.json.JSONObject;

public class GeminiChatCompletionWithFunctionCallingExample {

    public static void main(String[] args) {
        // Define a tool function
        GeminiToolDefinition weatherTool = GeminiToolDefinition.builder("get_weather")
                .description("Get current weather in a given location")
                .parameter("location", GeminiJsonSchema.stringSchema("City name"), true)
                .callback(ctx -> {
                    // Fake weather data
                    String loc = ctx.arguments().getString("location");
                    JSONObject result = new JSONObject()
                            .put("city", loc)
                            .put("forecast", "Sunny, 20 C");
                    return GeminiToolResult.of(result.toString());
                })
                .build();

        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Build request
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .addMessage("user", "What's the weather in Paris?")
                .addTool(weatherTool)
                .parallelToolCalls(true)
                .execute();

        // Print
        System.out.println("Gemini says: " + response.assistantMessage());
    }
}
