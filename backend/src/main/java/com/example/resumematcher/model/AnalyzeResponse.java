package com.example.resumematcher.model;

public record AnalyzeResponse(
        ResumeParseResponse resume,
        JobMatchResponse match
) {
}
