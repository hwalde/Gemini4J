package de.entwicklertraining.gemini4j;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Similar to GptJsonSchema, but for Gemini.
 * A structure that helps define the JSON schema for structured outputs.
 *
 * Important note: The Gemini "responseSchema" does NOT accept "additionalProperties"
 * in its JSON. Attempting to send that field triggers a "Cannot find field" error.
 * Therefore, we skip including that field in the final JSON.
 */
public sealed interface GeminiJsonSchema permits GeminiJsonSchemaImpl {

    JSONObject toJson();

    GeminiJsonSchema description(String desc);

    GeminiJsonSchema property(String name, GeminiJsonSchema schema, boolean requiredField);

    GeminiJsonSchema items(GeminiJsonSchema itemSchema);

    GeminiJsonSchema enumValues(String... values);

    /**
     * We keep the method for consistency, but it won't be written to JSON, because
     * the Gemini API doesn't allow "additionalProperties" in the schema.
     */
    GeminiJsonSchema additionalProperties(boolean allowed);

    static GeminiJsonSchema objectSchema() {
        return new GeminiJsonSchemaImpl("object");
    }

    static GeminiJsonSchema stringSchema(String description) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("string");
        schema.description(description);
        return schema;
    }

    static GeminiJsonSchema numberSchema(String description) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("number");
        schema.description(description);
        return schema;
    }

    static GeminiJsonSchema booleanSchema(String description) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("boolean");
        schema.description(description);
        return schema;
    }

    static GeminiJsonSchema integerSchema(String description) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("integer");
        schema.description(description);
        return schema;
    }

    static GeminiJsonSchema arraySchema(GeminiJsonSchema itemsSchema) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("array");
        schema.items(itemsSchema);
        return schema;
    }

    static GeminiJsonSchema enumSchema(String description, String... enumVals) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl("string");
        schema.description(description);
        schema.enumValues(enumVals);
        return schema;
    }

    static GeminiJsonSchema anyOf(GeminiJsonSchema... variants) {
        GeminiJsonSchemaImpl schema = new GeminiJsonSchemaImpl(null);
        schema.setAnyOfMode(true);
        for (GeminiJsonSchema variant : variants) {
            schema.getAnyOfSchemas().put(variant.toJson());
        }
        return schema;
    }
}

final class GeminiJsonSchemaImpl implements GeminiJsonSchema {

    private String type; // "object", "array", "string", etc. May be null in anyOfMode
    private String description;
    private final JSONObject properties;
    private final JSONArray required;
    private final JSONArray enumValues;
    private GeminiJsonSchema itemsSchema;
    private final JSONArray anyOfSchemas;
    private boolean additionalProperties; // we won't actually place this into the JSON
    private boolean anyOfMode;

    GeminiJsonSchemaImpl(String type) {
        this.type = type;
        this.description = null;
        this.properties = new JSONObject();
        this.required = new JSONArray();
        this.enumValues = new JSONArray();
        this.anyOfSchemas = new JSONArray();
        this.additionalProperties = false;
        this.anyOfMode = false;
    }

    @Override
    public GeminiJsonSchema description(String desc) {
        this.description = desc;
        return this;
    }

    @Override
    public GeminiJsonSchema property(String name, GeminiJsonSchema schema, boolean requiredField) {
        if (anyOfMode) {
            throw new IllegalStateException("Cannot add properties in anyOf mode directly.");
        }
        if (!"object".equals(type)) {
            throw new IllegalStateException("properties can only be added to an object schema.");
        }
        this.properties.put(name, schema.toJson());
        if (requiredField) {
            this.required.put(name);
        }
        return this;
    }

    @Override
    public GeminiJsonSchema items(GeminiJsonSchema itemSchema) {
        if (anyOfMode) {
            throw new IllegalStateException("Cannot set items in anyOf mode.");
        }
        if (!"array".equals(type)) {
            throw new IllegalStateException("items can only be set for array schemas.");
        }
        this.itemsSchema = itemSchema;
        return this;
    }

    @Override
    public GeminiJsonSchema enumValues(String... values) {
        if (anyOfMode) {
            throw new IllegalStateException("Cannot set enum in anyOf mode.");
        }
        if (type == null || !"string".equals(type)) {
            throw new IllegalStateException("enum is only supported on string schemas.");
        }
        for (String v : values) {
            enumValues.put(v);
        }
        return this;
    }

    @Override
    public GeminiJsonSchema additionalProperties(boolean allowed) {
        this.additionalProperties = allowed;
        return this;
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        // If we're in anyOf mode, we place "anyOf": [ ... ] and optional "description"
        if (anyOfMode) {
            obj.put("anyOf", anyOfSchemas);
            if (description != null && !description.isBlank()) {
                obj.put("description", description);
            }
            return obj;
        }

        // Otherwise, we have a normal schema
        if (type != null) {
            obj.put("type", type);
        }
        if (properties.length() > 0) {
            obj.put("properties", properties);
        }
        if (required.length() > 0) {
            obj.put("required", required);
        }
        if (enumValues.length() > 0) {
            obj.put("enum", enumValues);
        }
        if ("array".equals(type) && itemsSchema != null) {
            obj.put("items", itemsSchema.toJson());
        }
        // IMPORTANT: we do NOT put "additionalProperties" in the JSON,
        // because Gemini's API rejects that field.

        if (description != null && !description.isBlank()) {
            obj.put("description", description);
        }
        return obj;
    }

    void setAnyOfMode(boolean mode) {
        this.anyOfMode = mode;
    }

    JSONArray getAnyOfSchemas() {
        return anyOfSchemas;
    }
}
