package com.example.resumematcher.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class KeywordService {
    private static final Set<String> STOP_WORDS = Set.of(
            "负责", "熟悉", "掌握", "具备", "相关", "经验", "能力", "优先", "以及", "进行", "完成", "岗位", "要求", "工作",
            "the", "and", "with", "for"
    );
    private static final Pattern ENGLISH = Pattern.compile("[A-Za-z][A-Za-z0-9+#.\\-]{1,}");
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]{2,8}");
    private static final Pattern IMPORTANT = Pattern.compile("python|java|react|vue|redis|mysql|docker|linux|算法|后端|前端|大模型|机器学习|数据", Pattern.CASE_INSENSITIVE);

    public List<String> extractKeywords(String text) {
        Map<String, Double> scored = new LinkedHashMap<>();
        List<String> tokens = new ArrayList<>();
        collectMatches(ENGLISH, text, tokens);
        collectMatches(CHINESE, text, tokens);

        for (String token : tokens) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (STOP_WORDS.contains(normalized) || normalized.length() < 2) {
                continue;
            }
            double score = 1 + Math.log(token.length() + 1);
            if (IMPORTANT.matcher(normalized).find()) {
                score += 2;
            }
            scored.merge(token, score, Double::sum);
        }

        return scored.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(30)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void collectMatches(Pattern pattern, String text, List<String> tokens) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
    }
}
