package com.example.resumematcher.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.resumematcher.model.CleanTextResult;
import org.springframework.stereotype.Service;

@Service
public class TextCleanerService {
    public CleanTextResult clean(String rawText) {
        String text = rawText == null ? "" : rawText.replace("\r", "\n");
        text = text.replaceAll("\\x00", " ");
        text = text.replaceAll("(?i)Page\\s+\\d+\\s+of\\s+\\d+", " ");
        text = text.replaceAll("第\\s*\\d+\\s*/\\s*\\d+\\s*页", " ");
        text = text.replaceAll("[ \\t\\f\\x0B]+", " ");
        text = text.replaceAll(" *\\n *", "\n");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = normalizeBrokenLines(text);

        List<String> paragraphs = Arrays.stream(text.split("\\n{2,}"))
                .map(segment -> segment.replaceAll("^[\\s\\-•]+|[\\s\\-•]+$", ""))
                .filter(segment -> !segment.isBlank())
                .toList();
        return new CleanTextResult(String.join("\n\n", paragraphs).trim(), paragraphs);
    }

    private String normalizeBrokenLines(String text) {
        List<String> merged = new ArrayList<>();
        String buffer = "";
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                if (!buffer.isBlank()) {
                    merged.add(buffer.trim());
                    buffer = "";
                }
                merged.add("");
                continue;
            }
            if (!buffer.isBlank() && shouldMerge(buffer, line)) {
                buffer = buffer + " " + line;
            } else {
                if (!buffer.isBlank()) {
                    merged.add(buffer.trim());
                }
                buffer = line;
            }
        }
        if (!buffer.isBlank()) {
            merged.add(buffer.trim());
        }
        return String.join("\n", merged);
    }

    private boolean shouldMerge(String previous, String current) {
        if (previous.matches(".*[:：]$")) {
            return false;
        }
        if (current.matches("^(教育经历|工作经历|项目经历|技能|求职意向|自我评价|个人信息).*")) {
            return false;
        }
        if (previous.length() < 18) {
            return false;
        }
        return !previous.matches(".*[。.!?；;]$");
    }
}
