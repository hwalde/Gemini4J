package de.entwicklertraining.gemini4j;

import de.entwicklertraining.api.base.ApiResponse;
import org.json.JSONObject;

/**
 * Abstrakte Basis für Gemini-spezifische Responses,
 * erbt nun von ApiResponse<GeminiRequest<?>>.
 */
public abstract class GeminiResponse<T extends GeminiRequest<?>> extends ApiResponse<T> {

    protected final JSONObject json;  // In GeminiResponse wollen wir das JSON parsen/halten

    protected GeminiResponse(JSONObject json, T request) {
        super(request);
        this.json = json;
    }

    public JSONObject getJson() {
        return json;
    }
}
