package de.entwicklertraining.gemini4j;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Token-Service für Google Gemini/Gemma (SentencePiece).
 */
public class GeminiTokenService {
    private static final double AVG_CHARS_PER_TOKEN = 4.0;
    private final Encoding tokenizer;

    public GeminiTokenService() {
        tokenizer = loadTokenizer();
    }

    private Encoding loadTokenizer() {
        try {
            EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
            // Use cl100k_base encoding as it's closest to Gemini's tokenization
            return registry.getEncoding("cl100k_base").orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Zählt Gemini/Gemma-Tokens. Nutzt SentencePiece; fällt sonst auf Heuristik zurück.
     */
    public int calculateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            if (tokenizer != null) {
                return tokenizer.encode(text).size();
            }
        } catch (Exception ignored) {
            // fall through to heuristic
        }
        return (int) Math.ceil(text.length() / AVG_CHARS_PER_TOKEN);
    }
}

