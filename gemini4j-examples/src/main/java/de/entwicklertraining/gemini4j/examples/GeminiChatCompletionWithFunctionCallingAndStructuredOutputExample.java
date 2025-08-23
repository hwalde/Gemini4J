package de.entwicklertraining.gemini4j.examples;

import de.entwicklertraining.gemini4j.*;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionResponse;
import org.json.JSONObject;

import java.util.List;

/**
 * Example that uses both function calling (tools) and a final structured output.
 */
public class GeminiChatCompletionWithFunctionCallingAndStructuredOutputExample {

    public static record Weather(String city, String condition) {}
    public static record Meeting(String topic, String date, List<String> participants) {}
    public static record CombinedResult(Weather weather, Meeting meeting) {}

    public static void main(String[] args) {
        // Tools
        var weatherTool = GeminiToolDefinition.builder("fetchWeather")
                .description("Fetch weather for a city.")
                .parameter("city", GeminiJsonSchema.stringSchema("Name of the city"), true)
                .callback(ctx -> {
                    String city = ctx.arguments().getString("city");
                    JSONObject w = new JSONObject();
                    w.put("city", city);
                    w.put("condition", "Sunny");
                    return GeminiToolResult.of(w.toString());
                })
                .build();

        var meetingTool = GeminiToolDefinition.builder("scheduleMeeting")
                .description("Schedule a meeting.")
                .parameter("topic", GeminiJsonSchema.stringSchema("Topic of the meeting"), true)
                .parameter("date", GeminiJsonSchema.stringSchema("Date, e.g. 2025-01-01"), true)
                .parameter("participants", GeminiJsonSchema.arraySchema(
                        GeminiJsonSchema.stringSchema("Participant name")
                ), true)
                .callback(ctx -> {
                    String topic = ctx.arguments().getString("topic");
                    String date = ctx.arguments().getString("date");
                    var participants = ctx.arguments().getJSONArray("participants").toList();
                    JSONObject m = new JSONObject();
                    m.put("topic", topic);
                    m.put("date", date);
                    m.put("participants", participants);
                    return GeminiToolResult.of(m.toString());
                })
                .build();

        // Combined schema
        // combined => { "weather": {city, condition}, "meeting": {topic, date, participants[] } }
        var weatherSchema = GeminiJsonSchema.objectSchema()
                .property("city", GeminiJsonSchema.stringSchema("City name"), true)
                .property("condition", GeminiJsonSchema.stringSchema("Weather condition"), true)
                .additionalProperties(false);

        var meetingSchema = GeminiJsonSchema.objectSchema()
                .property("topic", GeminiJsonSchema.stringSchema("Meeting topic"), true)
                .property("date", GeminiJsonSchema.stringSchema("Meeting date"), true)
                .property("participants",
                        GeminiJsonSchema.arraySchema(
                                GeminiJsonSchema.stringSchema("Participant name")
                        ),
                        true
                )
                .additionalProperties(false);

        var combinedSchema = GeminiJsonSchema.objectSchema()
                .property("weather", weatherSchema, true)
                .property("meeting", meetingSchema, true)
                .additionalProperties(false);

        // Create the Gemini client
        GeminiClient client = new GeminiClient();
        
        // build request
        GeminiChatCompletionResponse response = client.chat().completion()
                .model("gemini-2.5-flash-lite")
                .systemInstruction("You can fetch weather and schedule a meeting, then output JSON that matches the combined schema.")
                .addMessage("user", "Get me the weather in Berlin, and schedule a meeting about 'Planning' on 2025-05-10 with Alice and Bob")
                .addTool(weatherTool)
                .addTool(meetingTool)
                .responseSchema(combinedSchema)
                .responseMimeType("application/json")
                .parallelToolCalls(true)
                .execute();

        // parse
        if (response.hasRefusal()) {
            System.err.println("Refusal: " + response.refusal());
            return;
        }

        CombinedResult combined = response.convertTo(CombinedResult.class);
        System.out.println("Weather => city: " + combined.weather().city() + ", condition: " + combined.weather().condition());
        System.out.println("Meeting => topic: " + combined.meeting().topic()
                + ", date: " + combined.meeting().date()
                + ", participants: " + combined.meeting().participants());
    }
}
