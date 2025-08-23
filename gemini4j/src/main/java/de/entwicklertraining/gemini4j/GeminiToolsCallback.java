package de.entwicklertraining.gemini4j;

/**
 * A functional interface for implementing the callback when Gemini calls a function.
 */
@FunctionalInterface
public interface GeminiToolsCallback {
    GeminiToolResult handle(GeminiToolCallContext context);
}
