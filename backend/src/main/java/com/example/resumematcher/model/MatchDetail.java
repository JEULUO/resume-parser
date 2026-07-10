package com.example.resumematcher.model;

import java.util.List;

public record MatchDetail(
        double score,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        double skillMatchRate,
        double experienceRelevance,
        String aiComment
) {
}
