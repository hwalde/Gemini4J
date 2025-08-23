package de.entwicklertraining.gemini4j;

import de.entwicklertraining.api.base.ApiClient;
import de.entwicklertraining.api.base.ApiClientSettings;
import de.entwicklertraining.gemini4j.chat.completion.GeminiChatCompletionRequest;

// Import exception classes
import static de.entwicklertraining.api.base.ApiClient.HTTP_400_RequestRejectedException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_403_PermissionDeniedException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_404_NotFoundException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_429_RateLimitOrQuotaException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_500_ServerErrorException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_503_ServerUnavailableException;
import static de.entwicklertraining.api.base.ApiClient.HTTP_504_ServerTimeoutException;

/**
 * Ein Client für die Gemini-API mit integrierter Rate-Limit-Prüfung.
 * Das Modell wird nicht im JSON-Body, sondern in der URL übergeben,
 * daher überschreiben wir die Extraktion.
 */
public final class GeminiClient extends ApiClient {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public GeminiClient() {
        this(ApiClientSettings.builder().build(), DEFAULT_BASE_URL);
    }

    public GeminiClient(ApiClientSettings settings) {
        this(settings, DEFAULT_BASE_URL);
    }

    public GeminiClient(ApiClientSettings settings, String customBaseUrl) {
        // Call super constructor with settings only
        super(settings);

        // Set base URL after super() call
        setBaseUrl(customBaseUrl);

        // if no API key is provided, try to read it from the environment variable
        if(settings.getBearerAuthenticationKey().isEmpty() && System.getenv("GEMINI_API_KEY") != null) {
            this.settings = this.settings.toBuilder().setBearerAuthenticationKey(System.getenv("GEMINI_API_KEY")).build();
        }

        // Register Gemini-specific HTTP status code exceptions
        registerStatusCodeException(400, HTTP_400_RequestRejectedException.class, "HTTP 400 (INVALID_ARGUMENT)", false);
        registerStatusCodeException(403, HTTP_403_PermissionDeniedException.class, "HTTP 403 (PERMISSION_DENIED)", false);
        registerStatusCodeException(404, HTTP_404_NotFoundException.class, "HTTP 404 (NOT_FOUND)", false);
        registerStatusCodeException(429, HTTP_429_RateLimitOrQuotaException.class, "HTTP 429 (RESOURCE_EXHAUSTED)", true);
        registerStatusCodeException(500, HTTP_500_ServerErrorException.class, "HTTP 500 (INTERNAL)", true);
        registerStatusCodeException(503, HTTP_503_ServerUnavailableException.class, "HTTP 503 (UNAVAILABLE)", true);
        registerStatusCodeException(504, HTTP_504_ServerTimeoutException.class, "HTTP 504 (DEADLINE_EXCEEDED)", false);
    }

    public GeminiChat chat() {
        return new GeminiChat(this);
    }

    public static class GeminiChat {
        private final GeminiClient client;

        public GeminiChat(GeminiClient client) {
            this.client = client;
        }

        public GeminiChatCompletionRequest.Builder completion() {
            return GeminiChatCompletionRequest.builder(client);
        }
    }

}
