package com.example.resumematcher.model;

import java.util.List;

public record ResumeParseResponse(
        String resumeId,
        String fileName,
        int pages,
        String cleanedText,
        List<String> paragraphs,
        ResumeKeyInfo keyInfo,
        boolean cacheHit
) {
    public ResumeParseResponse withCacheHit(boolean value) {
        return new ResumeParseResponse(resumeId, fileName, pages, cleanedText, paragraphs, keyInfo, value);
    }
}
