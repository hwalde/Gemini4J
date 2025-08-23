# Gemini4J

Gemini4J is a fluent Java wrapper for the [Gemini API](https://ai.google.dev/gemini-api/docs).
It builds on top of the lightweight [`api-base`](https://github.com/hwalde/api-base) library which
handles HTTP communication, authentication and exponential backoff. The goal is to provide a type safe
and convenient way to access Gemini services from Java, being as close to the raw API as possible.

> **A word from the author**
>
> I created this library because I was looking for a Java library that interacts with the Gemini API while staying as close to the raw API as possible—the official Java library does not. This implementation is fully compatible with Gemini but only includes the features I personally require. I maintain similar libraries for DeepSeek and Gemini, each in its own repository so usage remains explicit. Everything supported by the Java API works here.
>
> At the moment the library only covers the parts I need. Chat Completions are implemented with nearly every option, but many specialized endpoints—like Fine Tuning or Evals—are missing. If you need additional functionality, feel free to implement it yourself or submit a pull request and I will consider adding it.


## Features

* Chat Completions including tool calling, structured outputs and vision inputs
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
[function calling example](gemini4j-examples/src/main/java/de/entwicklertraining/gemini4j/examples/GeminiChatCompletionWithFunctionCallingExample.java)
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

### Vision Example

The [vision example](gemini4j-examples/src/main/java/de/entwicklertraining/gemini4j/examples/GeminiChatCompletionWithVisionExample.java)
demonstrates image analysis capabilities:

```java
GeminiClient client = new GeminiClient();
GeminiChatCompletionResponse response = client.chat().completion()
        .model("gemini-2.5-flash")
        .addUserMessage("What's in this image?")
        .addImageUrl("https://example.com/image.jpg")
        .execute();
System.out.println(response.assistantMessage());
```

See the `gemini4j-examples` module for more demonstrations including base64 images, structured outputs, and thinking mode.

### Configuring the Client

`GeminiClient` accepts an `ApiClientSettings` object for fine‑grained control over retries and timeouts. The API key can be configured directly and a hook can inspect each request before it is sent:

```java
ApiClientSettings settings = ApiClientSettings.builder()
        .setBearerAuthenticationKey("my api key")
        .beforeSend(req -> System.out.println("Sending " + req.getHttpMethod() + " " + req.getRelativeUrl()))
        .build();

GeminiClient client = new GeminiClient(settings);
```

## Project Structure

The library follows a clear structure:

* **`GeminiClient`** – entry point for all API calls. Extends `ApiClient` from *api-base*
  and registers error handling. Currently exposes the chat completion endpoint via `chat()`.
* **Request/Response classes** – located in the `chat.completion` package.
  Each request extends `GeminiRequest` and has an inner `Builder` that extends
  `ApiRequestBuilderBase` from *api-base*. Responses extend `GeminiResponse`.
* **Tool calling** – defined via `GeminiToolDefinition` and handled by
  `GeminiToolsCallback` and `GeminiToolCallContext`.
* **Structured outputs** – use `GeminiJsonSchema` for defining response schemas.
* **Token utilities** – `GeminiTokenService` counts tokens via `jtokkit`.

The `gemini4j-examples` module demonstrates various use cases and can be used as a quick start.

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
