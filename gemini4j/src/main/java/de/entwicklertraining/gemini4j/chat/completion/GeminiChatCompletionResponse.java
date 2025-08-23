package de.entwicklertraining.gemini4j.chat.completion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.entwicklertraining.gemini4j.GeminiResponse;
import de.entwicklertraining.api.base.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Wraps the JSON response from Gemini.
 * e.g.:
 * {
 *   "candidates": [
 *     {
 *       "content": { "parts": [...], "role":"model", "refusal":"...", ...},
 *       "finishReason":"STOP", ...
 *     }
 *   ],
 *   ...
 * }
 */
public final class GeminiChatCompletionResponse extends GeminiResponse<GeminiChatCompletionRequest> {

    public GeminiChatCompletionResponse(JSONObject json, GeminiChatCompletionRequest request) {
        super(json, request);
    }

    /**
     * Returns the first candidate's "content" -> "parts" as a single string if possible.
     * If you see "functionCall" only, that means the model is calling a function
     * and might not have normal text content.
     */
    public String assistantMessage() {
        try {
            JSONArray candidates = getJson().getJSONArray("candidates");
            JSONObject firstCand = candidates.getJSONObject(0);
            JSONObject contentObj = firstCand.getJSONObject("content");
            // gather "parts" -> each part might have "text", or "functionCall"
            JSONArray parts = contentObj.getJSONArray("parts");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.getJSONObject(i);
                if (p.has("text")) {
                    sb.append(p.getString("text"));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return the finishReason from the first candidate
     */
    public String finishReason() {
        try {
            JSONArray candidates = getJson().getJSONArray("candidates");
            JSONObject firstCand = candidates.getJSONObject(0);
            return firstCand.optString("finishReason", null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The "content" might have "refusal" => check if present
     */
    public boolean hasRefusal() {
        try {
            JSONArray candidates = getJson().getJSONArray("candidates");
            JSONObject firstCand = candidates.getJSONObject(0);
            JSONObject contentObj = firstCand.getJSONObject("content");
            return contentObj.has("refusal") && !contentObj.isNull("refusal");
        } catch (Exception e) {
            return false;
        }
    }

    public String refusal() {
        try {
            JSONObject contentObj = getJson()
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content");
            return contentObj.getString("refusal");
        } catch (Exception e) {
            return null;
        }
    }

    public void throwOnRefusal() {
        if (hasRefusal()) {
            throw new ApiClient.ApiResponseUnusableException("Model refused to comply: " + refusal());
        }
    }

    /**
     * If the user expects a structured JSON object from "assistantMessage", parse it
     */
    public JSONObject parsed() {
        return new JSONObject(Objects.requireNonNull(assistantMessage()));
    }

    public <T> T convertTo(Class<T> targetType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(assistantMessage(), targetType);
        } catch (JsonProcessingException e) {
            throw new ApiClient.ApiResponseUnusableException(
                    "Failed to parse the model's JSON into the expected structure/POJO: " + e.getMessage(),
                    e
            );
        }
    }
}
