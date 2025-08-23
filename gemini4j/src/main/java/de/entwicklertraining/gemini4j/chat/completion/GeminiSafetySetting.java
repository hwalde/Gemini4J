package de.entwicklertraining.gemini4j.chat.completion;

/**
 * Represents a single safety setting line for Gemini.
 * e.g. { "category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_ONLY_HIGH" }
 */
public record GeminiSafetySetting(String category, String threshold) {
}
