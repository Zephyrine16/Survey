package com.example.survey.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only canonical, bounded matrix ratings; malformed legacy text is ignored safely. */
public final class RatingResponseParser {
    private static final Pattern RESPONSE = Pattern.compile("^(.*?):\\s*([0-9]+)(?:\\s*\\((.*?)\\))?$");

    private RatingResponseParser() {}

    public static Optional<ParsedRating> parse(String response) {
        if (response == null || response.isBlank()) return Optional.empty();
        Matcher matcher = RESPONSE.matcher(response.trim());
        if (!matcher.matches()) return Optional.empty();
        try {
            int value = Integer.parseInt(matcher.group(2));
            if (value < 1 || value > 5) return Optional.empty();
            return Optional.of(new ParsedRating(matcher.group(1).trim(), value, matcher.group(3)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static boolean looksLikeRating(String response) {
        return response != null && RESPONSE.matcher(response.trim()).matches();
    }

    public record ParsedRating(String dimension, int value, String scaleLabel) {}
}
