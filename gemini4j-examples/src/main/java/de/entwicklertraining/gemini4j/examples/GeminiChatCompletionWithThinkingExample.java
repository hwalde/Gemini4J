package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;

/**
 * An example demonstrating the "thinking" feature in Gemini API.
 * 
 * The thinking feature allows the model to show its reasoning process before providing a final answer.
 * This can be useful for complex tasks where you want to see how the model arrived at its conclusion.
 * 
 * This example shows three different ways to use the thinking feature:
 * 1. Without thinking (disabled)
 * 2. With thinking enabled and a budget
 * 3. With thinking enabled but no budget specified
 */
public class GeminiChatCompletionWithThinkingExample {

    public static void main(String[] args) {
        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // Example 1: Without thinking (disabled by default)
        System.out.println("EXAMPLE 1: WITHOUT THINKING (DISABLED)");
        GeminiChatCompletionResponse response1 = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .addMessage("user", "Solve this math problem step by step: If a train travels at 120 km/h and another train travels at 80 km/h in the opposite direction, how long will it take for them to be 500 km apart if they start at the same location?")
                // No thinking method call means thinking is disabled
                .execute();

        System.out.println("Response without thinking:");
        System.out.println(response1.assistantMessage());
        System.out.println("\n-----------------------------------\n");

        // Example 2: With thinking enabled and a budget
        System.out.println("EXAMPLE 2: WITH THINKING ENABLED AND BUDGET");
        GeminiChatCompletionResponse response2 = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .addMessage("user", "Solve this math problem step by step: If a train travels at 120 km/h and another train travels at 80 km/h in the opposite direction, how long will it take for them to be 500 km apart if they start at the same location?")
                .thinking(1000) // Set thinking budget to 1000 tokens
                .execute();

        System.out.println("Response with thinking budget of 1000 tokens:");
        System.out.println(response2.assistantMessage());
        System.out.println("\n-----------------------------------\n");

        // Example 3: With thinking enabled but no budget specified
        System.out.println("EXAMPLE 3: WITH THINKING ENABLED BUT NO BUDGET");
        GeminiChatCompletionResponse response3 = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .addMessage("user", "Solve this math problem step by step: If a train travels at 120 km/h and another train travels at 80 km/h in the opposite direction, how long will it take for them to be 500 km apart if they start at the same location?")
                .thinking(null) // This does nothing according to the requirements
                .execute();

        System.out.println("Response with thinking enabled but no budget:");
        System.out.println(response3.assistantMessage());
    }
}