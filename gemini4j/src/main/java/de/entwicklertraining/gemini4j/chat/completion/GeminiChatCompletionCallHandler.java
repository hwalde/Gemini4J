package de.entwicklertraining.gemini4j.chat.completion;

import de.entwicklertraining.gemini4j.GeminiClient;
import de.entwicklertraining.gemini4j.GeminiToolCallContext;
import de.entwicklertraining.gemini4j.GeminiToolDefinition;
import de.entwicklertraining.api.base.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Similar to GptChatCompletionCallHandler, but adapted for Gemini.
 *  - We handle "functionCall" from the response inside content.parts
 *  - We stop after a certain number of turns to avoid infinite loops
 *  - We handle structured outputs or refusal if present
 */
public final class GeminiChatCompletionCallHandler {

    private static final int MAX_TURNS = 10;
    private final GeminiClient client;

    public GeminiChatCompletionCallHandler(GeminiClient client) {
        this.client = client;
    }

    public GeminiChatCompletionResponse handleRequest(GeminiChatCompletionRequest initialRequest, boolean useExponentialBackoff) {
        // Copy the initial messages and tools
        List<JSONObject> messages = new ArrayList<>(initialRequest.messages());
        var toolMap = new HashMap<String, GeminiToolDefinition>();
        for (var t : initialRequest.tools()) {
            toolMap.put(t.name(), t);
        }

        GeminiChatCompletionRequest currentRequest = initialRequest;
        int turnCount = 0;

        while (true) {
            turnCount++;
            if (turnCount > MAX_TURNS) {
                throw new ApiClient.ApiClientException("Exceeded maximum of " + MAX_TURNS + " Gemini call iterations without final stop.");
            }

            // Send the request
            GeminiChatCompletionResponse response;
            if(useExponentialBackoff) {
                response = client.sendRequestWithExponentialBackoff(currentRequest);
            } else {
                response = client.sendRequest(currentRequest);
            }

            // Check if there's an "error" field in JSON => throw invalid request
            if (response.getJson().has("error")) {
                throw new ApiClient.HTTP_400_RequestRejectedException(
                        "Gemini API returned an error: " + response.getJson().toString()
                );
            }

            String finishReason = response.finishReason();
            if (response.hasRefusal()) {
                // If the model refused => final
                return response;
            }

            // Instead of 'tool_calls', Gemini provides function calls in: candidates[0].content.parts[i].functionCall
            // We'll gather them into a list
            List<JSONObject> functionCalls = extractFunctionCalls(response.getJson());

            // Add the assistant/model message to the conversation
            // Because Gemini often uses "role":"model" (rather than "assistant")
            /*JSONObject assistantMessage = new JSONObject()
                    .put("role", "model")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", response.assistantMessage())));
            messages.add(assistantMessage);*/

            if (functionCalls.isEmpty()) {
                // No function calls => final response
                return response;
            }

            // If we do have function calls, process them

            var parts = new JSONArray();

            for (JSONObject fnCall : functionCalls) {
                String toolName = fnCall.optString("name", null);
                if (toolName == null || !toolMap.containsKey(toolName)) {
                    throw new ApiClient.ApiResponseUnusableException("Unknown or missing tool name: " + toolName);
                }

                JSONObject args;
                try {
                    // The arguments are in "args" object
                    args = fnCall.getJSONObject("args");
                } catch (Exception e) {
                    throw new ApiClient.ApiResponseUnusableException(
                            "Failed to parse tool call arguments. " + e.getMessage()
                    );
                }

                // Run the callback
                GeminiToolDefinition definition = toolMap.get(toolName);
                var result = definition.callback().handle(new GeminiToolCallContext(args));

                // Instead of a custom field like "content" or "tool_call_id",
                // we add a new message with "role"="user" (or system) and
                // a "parts" array containing the result text from the tool.
                // This feeds the next iteration in the conversation with that info.

                parts.put(
                    new JSONObject()
                            .put("function_response", new JSONObject()
                                    .put("name", toolName)
                                    .put("response", result.content())
                            )
                );
                //
            }

            messages.add(
                    response.getJson().getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
            );

            JSONObject toolResponse = new JSONObject()
                    .put("role", "user")
                    .put("parts", parts);
            messages.add(toolResponse);

            // build the next request
            currentRequest = buildNextRequest(initialRequest, messages);
        }
    }

    /**
     * Extract function calls from the structure:
     * {
     *   "candidates":[
     *     {
     *       "content":{
     *         "parts":[
     *           {"functionCall": { "name":"...", "args":{...} }},
     *           ...
     *         ]
     *       }
     *     }
     *   ]
     * }
     */
    private List<JSONObject> extractFunctionCalls(JSONObject responseJson) {
        List<JSONObject> functionCalls = new ArrayList<>();
        try {
            JSONArray candidates = responseJson.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.getJSONObject(i);
                if (p.has("functionCall")) {
                    JSONObject fnCall = p.getJSONObject("functionCall");
                    functionCalls.add(fnCall);
                }
            }
        } catch (Exception e) {
            // If it fails, we just return empty
        }
        return functionCalls;
    }

    private GeminiChatCompletionRequest buildNextRequest(
            GeminiChatCompletionRequest original, List<JSONObject> updatedMessages
    ) {
        var builder = GeminiChatCompletionRequest.builder(client)
                .model(original.model())
                .maxExecutionTimeInSeconds(original.getMaxExecutionTimeInSeconds())
                .setCancelSupplier(original.getIsCanceledSupplier())
                .temperature(original.temperature())
                .topK(original.topK())
                .topP(original.topP())
                .maxOutputTokens(original.maxOutputTokens())
                .stopSequences(original.stopSequences())
                .safetySettings(original.safetySettings())
                .tools(original.tools())
                .parallelToolCalls(original.parallelToolCalls())
                .responseSchema(original.responseSchema())
                .responseMimeType(original.responseMimeType())
                .systemInstruction(original.systemInstruction())
                .thinking(original.thinkingBudget())
                .addAllMessages(updatedMessages);

        // Übernehmen der captureOnSuccess / captureOnError
        if (original.hasCaptureOnSuccess()) {
            builder.captureOnSuccess(original.getCaptureOnSuccess());
        }
        if (original.hasCaptureOnError()) {
            builder.captureOnError(original.getCaptureOnError());
        }

        return builder.build();
    }
}
