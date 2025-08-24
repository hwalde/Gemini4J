package de.entwicklertraining.gemini4j;

import org.json.JSONObject;

/**
 * Encapsulates the output from a tool invocation.
 */
public record GeminiToolResult(JSONObject content) {

    public static GeminiToolResult of(JSONObject content) {
        return new GeminiToolResult(content);
    }
}
