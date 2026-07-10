package com.example.resumematcher.model;

import java.util.List;

public record JobMatchResponse(
        String resumeId,
        List<String> jobKeywords,
        double score,
        MatchDetail detail,
        boolean cacheHit
) {
    public JobMatchResponse withCacheHit(boolean value) {
        return new JobMatchResponse(resumeId, jobKeywords, score, detail, value);
    }
}
