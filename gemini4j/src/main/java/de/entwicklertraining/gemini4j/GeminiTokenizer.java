package de.entwicklertraining.gemini4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiTokenizer {
    private static GeminiTokenizer instance;

    // Regex to split tags from text:
    // - "<[^>]+>" matches any sequence that starts with < and ends with >
    // - "[^<]+" matches non-tag text between angle brackets
    private static final Pattern HTML_SPLIT_PATTERN = Pattern.compile("<[^>]+>|[^<]+");

    // Regex to further split text segments on punctuation or whitespace.
    // This pattern captures sequences of:
    //   1. word-characters (letters/digits/underscore) possibly including apostrophes or hyphens inside words
    //   2. individual punctuation characters as separate tokens
    //   3. HTML entities like &amp; or &#123; as separate tokens
    //   4. leftover tokens that don't match word chars or punctuation
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile(
            "&[a-zA-Z]+;|&#\\d+;|[\\p{L}\\p{M}\\p{N}]+(?:['-][\\p{L}\\p{M}\\p{N}]+)*|[\\p{Punct}]|\\S"
    );

    private GeminiTokenizer() {
        // private because of singleton
    }

    public static synchronized GeminiTokenizer getInstance() {
        if (instance == null) {
            instance = new GeminiTokenizer();
        }
        return instance;
    }

    /**
     * Counts tokens in the input using a two-step approach:
     *  1) Split HTML tags vs. text.
     *  2) For each portion:
     *     - if it's a tag, treat the entire tag as a token
     *     - if it's text, split further on punctuation, entities, words
     */
    public int countTokens(String html) {
        List<String> tokenList = tokenize(html);

        // Option A: Just return the raw count of tokens
        // return tokenList.size();

        // Option B: Approximate tokens by the 4-characters-per-token rule of Gemini
        //           but still split first, then sum approximate tokens for each sub-token.
        //           The sum below is often closer in spirit to how LLMs chunk text.
        int totalApproxTokens = 0;
        for (String t : tokenList) {
            // If you want to give a minimum of "1 token" even for small strings,
            // you could do something like:
            int length = t.length();
            int approxTokens = Math.max(1, (int) Math.ceil((double) length / 4));
            totalApproxTokens += approxTokens;
        }

        return totalApproxTokens;
    }

    /**
     * Returns the exact list of tokens identified by the custom logic.
     */
    private List<String> tokenize(String html) {
        List<String> tokens = new ArrayList<>();

        // 1) Split out tags from text using the HTML_SPLIT_PATTERN
        Matcher matcher = HTML_SPLIT_PATTERN.matcher(html);
        while (matcher.find()) {
            String segment = matcher.group();

            if (segment.startsWith("<") && segment.endsWith(">")) {
                // Entire HTML tag as one token:
                tokens.add(segment);
            } else {
                // This is a text segment, so we further split on punctuation/entities/words
                List<String> subTokens = splitTextSegment(segment);
                tokens.addAll(subTokens);
            }
        }

        return tokens;
    }

    /**
     * Split text segments on punctuation, entities, and words.
     */
    private List<String> splitTextSegment(String textSegment) {
        List<String> subTokens = new ArrayList<>();
        Matcher matcher = TOKEN_SPLIT_PATTERN.matcher(textSegment);
        while (matcher.find()) {
            subTokens.add(matcher.group());
        }
        return subTokens;
    }
}
