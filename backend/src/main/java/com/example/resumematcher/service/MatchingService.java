package com.example.resumematcher.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.resumematcher.model.AiScore;
import com.example.resumematcher.model.JobMatchResponse;
import com.example.resumematcher.model.MatchDetail;
import com.example.resumematcher.model.ResumeKeyInfo;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
    private final KeywordService keywordService;
    private final OpenAiService openAiService;

    public MatchingService(KeywordService keywordService, OpenAiService openAiService) {
        this.keywordService = keywordService;
        this.openAiService = openAiService;
    }

    public JobMatchResponse match(String resumeId, String resumeText, ResumeKeyInfo keyInfo, String jobDescription) {
        List<String> jobKeywords = keywordService.extractKeywords(jobDescription);
        Set<String> resumeKeywords = new HashSet<>();
        keywordService.extractKeywords(resumeText).forEach(keyword -> resumeKeywords.add(keyword.toLowerCase(Locale.ROOT)));
        keyInfo.getSkills().forEach(skill -> resumeKeywords.add(skill.toLowerCase(Locale.ROOT)));

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String keyword : jobKeywords) {
            if (resumeKeywords.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        double skillMatchRate = round4((double) matched.size() / Math.max(jobKeywords.size(), 1));
        double experienceRelevance = calculateExperienceRelevance(keyInfo, jobDescription);
        double score = (skillMatchRate * 70) + (experienceRelevance * 30);
        String aiComment = null;

        var aiScore = openAiService.scoreMatch(resumeText, keyInfo, jobDescription);
        if (aiScore.isPresent()) {
            AiScore value = aiScore.get();
            if (value.score() != null) {
                score = (score * 0.45) + (value.score() * 0.55);
            }
            aiComment = value.comment();
        }

        double finalScore = round2(Math.max(0, Math.min(100, score)));
        MatchDetail detail = new MatchDetail(
                finalScore,
                matched,
                missing.stream().limit(20).toList(),
                skillMatchRate,
                experienceRelevance,
                aiComment
        );
        return new JobMatchResponse(resumeId, jobKeywords, finalScore, detail, false);
    }

    private double calculateExperienceRelevance(ResumeKeyInfo keyInfo, String jobDescription) {
        int requiredYears = parseMaxYears(jobDescription);
        int actualYears = parseMaxYears(keyInfo.getYearsOfExperience());
        if (requiredYears == 0) {
            return (!keyInfo.getWorkExperiences().isEmpty() || !keyInfo.getProjectExperiences().isEmpty()) ? 0.75 : 0.45;
        }
        if (actualYears == 0) {
            return 0.35;
        }
        return round4(Math.min(actualYears / (double) requiredYears, 1.2) / 1.2);
    }

    private int parseMaxYears(String text) {
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*年").matcher(text == null ? "" : text);
        int max = 0;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        return max;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
