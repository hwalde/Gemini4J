package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.GeminiJsonSchema;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

/**
 * Demonstrates returning structured JSON from Gemini by specifying a responseSchema
 */
public class GeminiChatCompletionWithStructuredOutputExample {

    public record MyRecipe(String name, int servings) {}

    public static void main(String[] args) {
        // Build a simple schema
        // Expect: { "name":"Chocolate Cake", "servings":4 }
        GeminiJsonSchema recipeSchema = GeminiJsonSchema.objectSchema()
                .property("name", GeminiJsonSchema.stringSchema("Name of the recipe"), true)
                .property("servings", GeminiJsonSchema.integerSchema("Number of servings"), true)
                .additionalProperties(false);

        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // We'll request JSON output
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite")// "gemini-2.5-flash-lite")
                .responseSchema(recipeSchema)
                .responseMimeType("application/json")
                .addMessage("user", "I want to eat 3 portions of smashed potatoes.")
                .execute();

        // If we trust the model obeyed:
        MyRecipe recipe = response.convertTo(MyRecipe.class);
        System.out.println("Recipe => name: " + recipe.name() + ", servings: " + recipe.servings());
    }
}
