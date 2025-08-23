package de.entwicklertraining.gemini4j;

/**
 * Encapsulates the output from a tool invocation.
 */
public record GeminiToolResult(String content) {

    public static GeminiToolResult of(String content) {
        return new GeminiToolResult(content);
    }
}
