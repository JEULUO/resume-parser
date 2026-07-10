package com.example.resumematcher.model;

import java.util.List;

public record CleanTextResult(String cleanedText, List<String> paragraphs) {
}
