Wichtig: Aktualisiere AGENTS.md nach jedem Task.
Wichtig: Aktualisiere die README.md nach jedem Task nur wenn die Informationen darin veraltet sind

# Gemini4J
Gemini4J is a fluent Java wrapper for the [Gemini API](https://platform.gemini.com/docs/api-reference).
It builds on top of the lightweight [`api-base`](https://github.com/hwalde/api-base) library which
handles HTTP communication, authentication and exponential backoff. The goal is to provide a type safe
and convenient way to access Gemini services from modern Java (JDK 21+).

## Features

* Chat Completions including tool calling, structured outputs and vision inputs
* Image generation with Gemini's Imagen models
* Embeddings with helper for cosine similarity
* Vision capabilities for image understanding and analysis
* Token counting utilities via `jtokkit`
* Fluent builder APIs for all requests
* Examples demonstrating each feature

## Installation

Add the dependency from Maven Central:

```xml
<dependency>
    <groupId>de.entwicklertraining</groupId>
    <artifactId>gemini4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Basic Usage

Instantiate a `GeminiClient` and use the builders exposed by its fluent API. The
[function calling example](examples/src/main/java/de/entwicklertraining/gemini4j/examples/GeminiChatCompletionWithFunctionCallingExample.java)
shows how tools can be defined and executed:

```java
GeminiToolDefinition weatherFunction = GeminiToolDefinition.builder("get_local_weather")
        .description("Get weather information for a location.")
        .parameter("location", GeminiJsonSchema.stringSchema("Name of the city"), true)
        .callback(ctx -> {
            String loc = ctx.arguments().getString("location");
            return GeminiToolResult.of("Sunny in " + loc + " with a high of 25°C.");
        })
        .build();

GeminiClient client = new GeminiClient(); // this will read the API key from the environment variable GEMINI_API_KEY

GeminiChatCompletionResponse resp = client.chat().completion()
        .model("gemini-2.5-flash")
        .addSystemMessage("You are a helpful assistant.")
        .addUserMessage("What's the weather in Berlin and the current time?")
        .addTool(weatherFunction)
        .execute();
System.out.println(resp.assistantMessage());
```
【F:examples/src/main/java/de/entwicklertraining/gemini4j/examples/GeminiChatCompletionWithFunctionCallingExample.java†L10-L44】

Image generation through natural language prompts in chat completions:

```java
GeminiClient client = new GeminiClient();
GeminiChatCompletionResponse response = client.chat().completion()
        .model("gemini-1.5-pro")
        .addUserMessage("Generate an image of a futuristic city floating in the sky, with neon lights")
        .execute();
System.out.println(response.assistantMessage());
```
【F:examples/src/main/java/de/entwicklertraining/gemini4j/examples/DallE3Example.java†L26-L43】

See the `examples` module for more demonstrations (embeddings, speech, translation, web search
and vision).

## Project Structure

The library follows a clear structure:

* **`GeminiClient`** – entry point for all API calls. Extends `ApiClient` from *api-base*
  and registers error handling. It exposes sub clients (`chat()`, `images()`, `audio()`, `embeddings()`).
* **Request/Response classes** – located in packages like
  `chat.completion`, `images.generations`, `audio.*`, `embeddings`.
  Each request extends `GeminiRequest` and has an inner `Builder` that extends
  `ApiRequestBuilderBase` from *api-base*. Responses extend `GeminiResponse`.
* **Tool calling** – defined via `GeminiToolDefinition` and handled by
  `GeminiToolsCallback` and `GeminiToolCallContext`.
* **Structured outputs** – use `GeminiJsonSchema` and `GeminiResponseFormat`.
* **Token utilities** – `GeminiTokenService` counts tokens via `jtokkit`.

The examples directory mirrors these packages and can be used as a quick start.

## Extending Gemini4J

1. **Create a Request** – subclass `GeminiRequest` and implement `getRelativeUrl`,
   `getHttpMethod`, `getBody` and `createResponse`. Provide a nested builder
   extending `ApiRequestBuilderBase`.
2. **Create a Response** – subclass `GeminiResponse` and parse the JSON or binary
   payload returned by Gemini.
3. **Expose a builder** – add a convenience method in `GeminiClient` returning your
   new builder so users can call it fluently.

Thanks to *api-base*, sending the request is handled by calling
`client.sendRequest(request)` or by using the builder’s `execute()` method which
internally delegates to `sendRequest` with optional exponential backoff.
See [api-base’s Readme](https://github.com/hwalde/api-base) for details on available
settings like retries, timeouts or capture hooks.

## Building

This project uses Maven. Compile the library and run examples with:

```bash
mvn package
```

## License

Gemini4J is distributed under the MIT License as defined in the project `pom.xml`.


## Additional Details

This Maven-based project targets JDK 21 and provides a fluent Java wrapper around the Gemini REST API. The main module `gemini4j` exposes builders for chat completions, image generation, embeddings and audio endpoints. Example usages are located in `examples/src/main/java/de/entwicklertraining/gemini4j/examples`.

Important packages include:
- `chat.completion` – classes like `GeminiChatCompletionRequest` and `GeminiChatCompletionResponse` implement the Chat Completions API.
The project has automated tests, and can be compiled with `mvn package`. The examples module depends on `gemini4j` and demonstrates features such as tool calling, vision capabilities and text generation.

## Maintenance
- Run `mvn package` to ensure the project builds. There are no tests.
- Keep this AGENTS.md file in sync with repository changes.
- Only update README.md if the information becomes outdated.

### Recent Changes
* Added an important notice at the beginning of README about supported Gemini features.
* Expanded README with new examples including ApiClientSettings usage and embeddings.

Wichtig: Aktualisiere AGENTS.md nach jedem Task.
Wichtig: Aktualisiere die README.md nach jedem Task nur wenn die Informationen darin veraltet sind
