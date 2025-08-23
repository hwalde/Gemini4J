package de.entwicklertraining.gemini4j.chat.completion;

import de.entwicklertraining.gemini4j.*;
import de.entwicklertraining.api.base.ApiRequestBuilderBase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * A request to call the Gemini generateContent endpoint:
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={API_KEY}
 *
 * We wrap fields like temperature, topK, topP, maxOutputTokens, etc.
 * We also handle "tools" for function calling, "safetySettings", "responseSchema", "responseMimeType", etc.
 *
 * IMPORTANT CHANGE:
 *  - The official Gemini doc shows "tools" is an array of objects,
 *    each containing "functionDeclarations": [ {...}, ... ].
 *  - Also "toolConfig": { "functionCallingConfig": { "mode": "ANY", ... } }
 *  - So we must produce EXACTLY that shape to avoid "Cannot find field" errors.
 */
public final class GeminiChatCompletionRequest extends GeminiRequest<GeminiChatCompletionResponse> {

    private final String model;
    private final Double temperature; // 0..2
    private final Integer topK;      // up to ?
    private final Double topP;       // 0..1
    private final Integer maxOutputTokens; // up to 8192 or more
    private final List<String> stopSequences;
    private final List<JSONObject> messages;
    private final List<GeminiToolDefinition> tools;
    private final List<GeminiSafetySetting> safetySettings;
    private final Boolean parallelToolCalls;
    private final GeminiJsonSchema responseSchema;
    private final String responseMimeType;
    private final String systemInstruction;
    private final Integer thinkingBudget; // Budget for thinking feature, null means disabled

    GeminiChatCompletionRequest(
            Builder builder,
            String model,
            Double temperature,
            Integer topK,
            Double topP,
            Integer maxOutputTokens,
            List<String> stopSequences,
            List<JSONObject> messages,
            List<GeminiToolDefinition> tools,
            List<GeminiSafetySetting> safetySettings,
            Boolean parallelToolCalls,
            GeminiJsonSchema responseSchema,
            String responseMimeType,
            String systemInstruction,
            Integer thinkingBudget
    ) {
        super(builder); // Neuer Parameter im Super-Konstruktor
        this.model = model;
        this.temperature = temperature;
        this.topK = topK;
        this.topP = topP;
        this.maxOutputTokens = maxOutputTokens;
        this.stopSequences = stopSequences;
        this.messages = messages;
        this.tools = tools;
        this.safetySettings = safetySettings;
        this.parallelToolCalls = parallelToolCalls;
        this.responseSchema = responseSchema;
        this.responseMimeType = responseMimeType;
        this.systemInstruction = systemInstruction;
        this.thinkingBudget = thinkingBudget;
    }

    public String model() {
        return model;
    }

    public Double temperature() {
        return temperature;
    }

    public Integer topK() {
        return topK;
    }

    public Double topP() {
        return topP;
    }

    public Integer maxOutputTokens() {
        return maxOutputTokens;
    }

    public List<String> stopSequences() {
        return stopSequences;
    }

    public List<JSONObject> messages() {
        return messages;
    }

    public List<GeminiToolDefinition> tools() {
        return tools;
    }

    public List<GeminiSafetySetting> safetySettings() {
        return safetySettings;
    }

    public Boolean parallelToolCalls() {
        return parallelToolCalls;
    }

    public GeminiJsonSchema responseSchema() {
        return responseSchema;
    }

    public String responseMimeType() {
        return responseMimeType;
    }

    public String systemInstruction() {
        return systemInstruction;
    }

    public Integer thinkingBudget() {
        return thinkingBudget;
    }

    public boolean isCaptureOnSuccess() {
        return false; // Capture functionality not available in current api-base version
    }

    public boolean isCaptureOnError() {
        return false; // Capture functionality not available in current api-base version
    }

    @Override
    public String getRelativeUrl() {
        // Return the relative URL path for the Gemini API endpoint
        return "/v1beta/models/" + model + ":generateContent";
    }
    

    @Override
    public String getHttpMethod() {
        return "POST";
    }

    @Override
    public String getBody() {
        JSONObject root = new JSONObject();

        // systemInstruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            JSONObject sysObj = new JSONObject();
            sysObj.put("role", "user");
            sysObj.put("parts", new JSONArray().put(new JSONObject().put("text", systemInstruction)));
            root.put("systemInstruction", sysObj);
        }


        // contents: the conversation messages
        JSONArray contentsArr = new JSONArray();
        for (JSONObject msg : messages) {
            contentsArr.put(msg);
        }
        root.put("contents", contentsArr);

        // safetySettings
        if (!safetySettings.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (GeminiSafetySetting setting : safetySettings) {
                JSONObject s = new JSONObject();
                s.put("category", setting.category());
                s.put("threshold", setting.threshold());
                arr.put(s);
            }
            root.put("safetySettings", arr);
        }

        // generationConfig
        if (temperature != null || topK != null || topP != null || maxOutputTokens != null
                || (stopSequences != null && !stopSequences.isEmpty())
                || (responseMimeType != null && !responseMimeType.isBlank())
                || responseSchema != null) {
            JSONObject genConfig = new JSONObject();
            if (temperature != null) {
                genConfig.put("temperature", temperature);
            }
            if (topK != null) {
                genConfig.put("topK", topK);
            }
            if (topP != null) {
                genConfig.put("topP", topP);
            }
            if (maxOutputTokens != null) {
                genConfig.put("maxOutputTokens", maxOutputTokens);
            }
            if (stopSequences != null && !stopSequences.isEmpty()) {
                JSONArray stp = new JSONArray();
                for (String s : stopSequences) {
                    stp.put(s);
                }
                genConfig.put("stopSequences", stp);
            }
            if (responseMimeType != null && !responseMimeType.isBlank()) {
                genConfig.put("responseMimeType", responseMimeType);
            }
            if (responseSchema != null) {
                genConfig.put("responseSchema", responseSchema.toJson());
            }

            // Add thinkingConfig to generationConfig
            if (thinkingBudget != null) {
                JSONObject thinkingConfig = new JSONObject();
                thinkingConfig.put("thinkingBudget", thinkingBudget);
                genConfig.put("thinkingConfig", thinkingConfig);
            }

            root.put("generationConfig", genConfig);
        }

        // Tools
        if (!tools.isEmpty()) {
            // The official doc shows:
            //   "tools": [
            //     {
            //       "functionDeclarations": [ {...}, {...} ]
            //     }
            //   ],
            // and "toolConfig": { "functionCallingConfig": { "mode": "ANY" } }
            //
            // We'll gather functionDeclarations from each GeminiToolDefinition
            JSONArray functionDeclarationsArray = new JSONArray();
            for (GeminiToolDefinition def : tools) {
                functionDeclarationsArray.put(def.toJson());
            }

            // "tools":[ { "functionDeclarations":[ ... ] } ]
            JSONArray toolsArr = new JSONArray();
            JSONObject toolObject = new JSONObject();
            toolObject.put("functionDeclarations", functionDeclarationsArray);
            toolsArr.put(toolObject);
            root.put("tools", toolsArr);

            // "toolConfig": { "functionCallingConfig": { "mode": "ANY"|"AUTO"|"NONE" } }
            // If parallelToolCalls == true, let's pick "ANY", else "AUTO" (just an example).
            JSONObject toolConfigObj = new JSONObject();
            JSONObject funcCallingConfigObj = new JSONObject();
            if (parallelToolCalls != null && parallelToolCalls) {
                funcCallingConfigObj.put("mode", "ANY");
            } else {
                // default: "AUTO"
                funcCallingConfigObj.put("mode", "AUTO");
            }
            toolConfigObj.put("functionCallingConfig", funcCallingConfigObj);
            root.put("toolConfig", toolConfigObj);
        }

        return root.toString();
    }

    @Override
    public GeminiChatCompletionResponse createResponse(String responseBody) {
        return new GeminiChatCompletionResponse(new JSONObject(responseBody), this);
    }


    public static Builder builder(GeminiClient client) {
        return new Builder(client);
    }

    public static final class Builder extends ApiRequestBuilderBase<Builder, GeminiChatCompletionRequest> {
        private final GeminiClient client;
        private String model = "gemini-1.5-flash";
        private Double temperature;
        private Integer topK;
        private Double topP;
        private Integer maxOutputTokens;
        private final List<String> stopSequences = new ArrayList<>();
        private final List<JSONObject> messages = new ArrayList<>();
        private final List<GeminiToolDefinition> tools = new ArrayList<>();
        private final List<GeminiSafetySetting> safetySettings = new ArrayList<>();
        private Boolean parallelToolCalls;
        private GeminiJsonSchema responseSchema;
        private String responseMimeType;
        private String systemInstruction;
        private Integer thinkingBudget;
        private boolean captureOnSuccess = false;
        private boolean captureOnError = false;

        // For extension checks
        private static final Set<String> ALLOWED_EXTENSIONS =
                Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

        public Builder(GeminiClient client) {
            this.client = client;
        }

        public Builder model(String m) {
            this.model = m;
            return this;
        }

        public Builder temperature(Double t) {
            this.temperature = t;
            return this;
        }

        public Builder topK(Integer k) {
            this.topK = k;
            return this;
        }

        public Builder topP(Double p) {
            this.topP = p;
            return this;
        }

        public Builder maxOutputTokens(Integer m) {
            this.maxOutputTokens = m;
            return this;
        }

        public Builder stopSequences(List<String> stops) {
            this.stopSequences.addAll(stops);
            return this;
        }

        public Builder addStopSequence(String stop) {
            this.stopSequences.add(stop);
            return this;
        }

        public Builder addMessage(String role, String text) {
            JSONObject msg = new JSONObject();
            msg.put("role", role);
            JSONArray parts;
            if(msg.has("parts")) {
                parts = msg.getJSONArray("parts");
            } else {
                parts = new JSONArray();
            }
            JSONObject partObj = new JSONObject().put("text", text);
            parts.put(partObj);
            msg.put("parts", parts);
            messages.add(msg);
            return this;
        }

        public Builder addAllMessages(List<JSONObject> msgList) {
            this.messages.addAll(msgList);
            return this;
        }

        public Builder tools(List<GeminiToolDefinition> t) {
            this.tools.addAll(t);
            return this;
        }

        public Builder addTool(GeminiToolDefinition t) {
            this.tools.add(t);
            return this;
        }

        public Builder safetySettings(List<GeminiSafetySetting> set) {
            this.safetySettings.addAll(set);
            return this;
        }

        /**
         * If true => we set functionCallingConfig.mode=ANY,
         * else => functionCallingConfig.mode=AUTO (by default).
         */
        public Builder parallelToolCalls(Boolean allow) {
            this.parallelToolCalls = allow;
            return this;
        }

        public Builder responseSchema(GeminiJsonSchema schema) {
            this.responseSchema = schema;
            return this;
        }

        public Builder responseMimeType(String mime) {
            this.responseMimeType = mime;
            return this;
        }

        /**
         * This puts a systemInstruction as a user message in "systemInstruction"
         * so that Gemini treats it as additional context (similar to system role).
         */
        public Builder systemInstruction(String instruction) {
            this.systemInstruction = instruction;
            return this;
        }

        /**
         * Sets the thinking budget for the model.
         * If not set, thinking is disabled.
         * If set with a budget, it sets the thinking budget.
         * If set without a budget (null), it does nothing.
         * 
         * @param budget The thinking budget or null
         * @return This builder for chaining
         */
        public Builder thinking(Integer budget) {
            this.thinkingBudget = budget;
            return this;
        }

        public Builder captureOnSuccess(java.util.function.Consumer<de.entwicklertraining.api.base.ApiCallCaptureInput> captureConsumer) {
            this.captureOnSuccess = true;
            // Store the consumer if needed - for now just enable the flag
            return this;
        }

        public Builder captureOnError(java.util.function.Consumer<de.entwicklertraining.api.base.ApiCallCaptureInput> captureConsumer) {
            this.captureOnError = true;
            // Store the consumer if needed - for now just enable the flag
            return this;
        }

        /**
         * Adds an image via external URL. Validates supported file extensions:
         * - png, jpg, jpeg, webp, heic, heif
         * The image is added as a user message with a part containing the image URL.
         * 
         * @param url The URL of the image
         * @return This builder for chaining
         * @throws IllegalArgumentException if the URL has an unsupported file extension
         * @throws RuntimeException if there's an error downloading the image
         */
        public Builder addImageByUrl(String url) {
            Objects.requireNonNull(url, "url must not be null");

            String fileExt = extractExtension(url).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(fileExt)) {
                throw new IllegalArgumentException(
                        "Unsupported file extension: " + fileExt + ". Allowed: " + ALLOWED_EXTENSIONS
                );
            }

            String mimeType = extensionToMime(fileExt);

            // Download the image and convert to base64
            byte[] imageBytes;
            try {
                java.net.URL imageUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) imageUrl.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (java.io.InputStream in = connection.getInputStream()) {
                    imageBytes = in.readAllBytes();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to download image from URL: " + url + " => " + e.getMessage(), e);
            }
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            // Create a user message with a part containing the base64-encoded image
            JSONObject msg = new JSONObject();
            msg.put("role", "user");

            JSONArray parts;
            if(msg.has("parts")) {
                parts = msg.getJSONArray("parts");
            } else {
                parts = new JSONArray();
            }

            // Add the image part as inline_data instead of file_uri
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Data);
            imagePart.put("inline_data", inlineData);
            parts.put(imagePart);

            msg.put("parts", parts);
            messages.add(msg);

            return this;
        }

        /**
         * Reads a local image file, base64-encodes it, and adds it as a user message.
         * Validates supported file extensions: png, jpg, jpeg, webp, heic, heif.
         * 
         * @param filePath The path to the local image file
         * @return This builder for chaining
         * @throws IllegalArgumentException if the file has an unsupported extension
         * @throws RuntimeException if there's an error reading the file
         */
        public Builder addImageByBase64(Path filePath) {
            Objects.requireNonNull(filePath, "filePath must not be null");

            String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
            String ext = extractExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException(
                        "Unsupported file extension: " + ext + ". Allowed: " + ALLOWED_EXTENSIONS
                );
            }

            String mimeType = extensionToMime(ext);

            byte[] fileBytes;
            try {
                fileBytes = Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + filePath + " => " + e.getMessage(), e);
            }
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            // Create a user message with a part containing the base64-encoded image
            JSONObject msg = new JSONObject();
            msg.put("role", "user");

            JSONArray parts;
            if(msg.has("parts")) {
                parts = msg.getJSONArray("parts");
            } else {
                parts = new JSONArray();
            }

            // Add the image part
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Data);
            imagePart.put("inline_data", inlineData);
            parts.put(imagePart);

            msg.put("parts", parts);
            messages.add(msg);

            return this;
        }

        /**
         * Extracts the file extension from a path or URL.
         * 
         * @param path The path or URL
         * @return The file extension, or an empty string if none is found
         */
        private static String extractExtension(String path) {
            int dotIdx = path.lastIndexOf('.');
            if (dotIdx < 0) {
                return "";
            }
            String raw = path.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
            // strip query params if any
            int qMark = raw.indexOf('?');
            return (qMark >= 0) ? raw.substring(0, qMark) : raw;
        }

        /**
         * Converts a file extension to a MIME type.
         * 
         * @param ext The file extension
         * @return The corresponding MIME type
         * @throws IllegalArgumentException if the extension is not supported
         */
        private static String extensionToMime(String ext) {
            return switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                case "heic" -> "image/heic";
                case "heif" -> "image/heif";
                default -> throw new IllegalArgumentException("Unsupported extension (mime lookup) " + ext);
            };
        }

        public GeminiChatCompletionRequest build() {
            return new GeminiChatCompletionRequest(
                    this,
                    model,
                    temperature,
                    topK,
                    topP,
                    maxOutputTokens,
                    List.copyOf(stopSequences),
                    List.copyOf(messages),
                    List.copyOf(tools),
                    List.copyOf(safetySettings),
                    parallelToolCalls,
                    responseSchema,
                    responseMimeType,
                    systemInstruction,
                    thinkingBudget
            );
        }

        @Override
        public GeminiChatCompletionResponse execute() {
            return new GeminiChatCompletionCallHandler(client).handleRequest(build(), false);
        }

        @Override
        public GeminiChatCompletionResponse executeWithExponentialBackoff() {
            return new GeminiChatCompletionCallHandler(client).handleRequest(build(), true);
        }
    }
}
