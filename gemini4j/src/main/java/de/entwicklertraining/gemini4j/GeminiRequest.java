package de.entwicklertraining.gemini4j;

import de.entwicklertraining.api.base.ApiRequest;
import de.entwicklertraining.api.base.ApiRequestBuilderBase;

/**
 * Eine abstrakte Gemini-spezifische Request-Klasse,
 * die nun von ApiRequest<T> erbt.
 */
public abstract class GeminiRequest<T extends GeminiResponse<?>> extends ApiRequest<T> {

    protected <Y extends ApiRequestBuilderBase<?, ?>> GeminiRequest(Y builder) {
        super(builder);
    }

    /**
     * @return z.B. "POST" oder "GET".
     */
    @Override
    public abstract String getHttpMethod();

    /**
     * @return Der JSON-Body (String) für diesen Request.
     */
    @Override
    public abstract String getBody();

    /**
     * Erzeugt die passende GeminiResponse-Subklasse aus dem JSON-String.
     */
    @Override
    public abstract T createResponse(String responseBody);

    // Da wir isBinaryResponse, getBodyBytes etc. ggf. überschreiben können,
    // lassen wir sie hier unverändert. Standard-Implementierung reicht oft aus.
}
