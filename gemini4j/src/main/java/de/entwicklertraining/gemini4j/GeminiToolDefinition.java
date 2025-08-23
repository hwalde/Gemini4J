package de.entwicklertraining.gemini4j;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Similar to GptToolDefinition, but adapted for Gemini's function-calling style.
 *
 * IMPORTANT CHANGE:
 *  - The Gemini API expects an array of "functionDeclarations" objects, each with "name", "description", "parameters".
 *  - We must NOT nest them under "function": {...}, nor must we place a "type":"function" key there,
 *    otherwise we get "Cannot find field." errors.
 *  - Also remove "strict".
 */
public final class GeminiToolDefinition {

    private final String name;
    private final String description;
    private final JSONObject parameters;
    private final GeminiToolsCallback callback;

    private GeminiToolDefinition(
            String name,
            String description,
            JSONObject parameters,
            GeminiToolsCallback callback
    ) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.callback = callback;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public JSONObject parameters() {
        return parameters;
    }

    public GeminiToolsCallback callback() {
        return callback;
    }

    /**
     * According to the updated Gemini function calling docs, each "functionDeclarations" item
     * should have "name", "description", and "parameters".
     * So we produce that structure here directly (no extra "function", no "type").
     */
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("description", description);
        obj.put("parameters", parameters);
        return obj;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private String description;
        private final JSONObject schema = new JSONObject();
        private final JSONObject properties = new JSONObject();
        private final JSONArray required = new JSONArray();
        private GeminiToolsCallback callback;

        private Builder(String name) {
            this.name = name;
            schema.put("type", "object");
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder parameter(String paramName, GeminiJsonSchema paramSchema, boolean requiredField) {
            properties.put(paramName, paramSchema.toJson());
            if (requiredField) {
                required.put(paramName);
            }
            return this;
        }

        public Builder callback(GeminiToolsCallback cb) {
            this.callback = cb;
            return this;
        }

        public GeminiToolDefinition build() {
            if (!properties.isEmpty()) {
                schema.put("properties", properties);
            }
            if (!required.isEmpty()) {
                schema.put("required", required);
            }

            return new GeminiToolDefinition(name, description, schema, callback);
        }
    }
}
