package com.example.resumematcher.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.resumematcher.config.AppProperties;
import com.example.resumematcher.model.AiScore;
import com.example.resumematcher.model.ResumeKeyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {
    private static final URI CHAT_COMPLETIONS_URL = URI.create("https://api.openai.com/v1/chat/completions");

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public Optional<ResumeKeyInfo> extractResumeInfo(String text) {
        if (!appProperties.hasOpenAiKey()) {
            return Optional.empty();
        }
        String prompt = """
                你是严谨的简历解析助手。请从简历文本中提取结构化信息，只返回 JSON。
                字段必须包含：
                name, phone, email, address, job_intention, expected_salary, years_of_experience,
                education_background, project_experiences, skills, work_experiences。
                无法确定的字符串字段返回 null，列表字段返回 []。

                简历文本：
                %s
                """.formatted(limit(text, 12000));

        return chat(prompt, "你只输出可被 JSON 解析的对象，不要输出 Markdown。")
                .flatMap(content -> readJson(content, ResumeKeyInfo.class));
    }

    public Optional<AiScore> scoreMatch(String resumeText, ResumeKeyInfo keyInfo, String jobDescription) {
        if (!appProperties.hasOpenAiKey()) {
            return Optional.empty();
        }
        String prompt = """
                请评估候选人与岗位的匹配度。只返回 JSON：
                {"score": 0-100之间数字, "comment": "一句话说明主要匹配点和风险"}

                岗位需求：
                %s

                结构化简历：
                %s

                简历文本：
                %s
                """.formatted(limit(jobDescription, 5000), toJson(keyInfo), limit(resumeText, 9000));

        return chat(prompt, "你是招聘匹配评分助手，只输出 JSON。")
                .flatMap(content -> readJson(content, AiScore.class));
    }

    private Optional<String> chat(String userPrompt, String systemPrompt) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", appProperties.openaiModel(),
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            HttpRequest request = HttpRequest.newBuilder(CHAT_COMPLETIONS_URL)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + appProperties.openaiApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return content.isBlank() ? Optional.empty() : Optional.of(content);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private <T> Optional<T> readJson(String content, Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(stripJsonFence(content), type));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String stripJsonFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").trim();
            trimmed = trimmed.replaceFirst("```$", "").trim();
        }
        return trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
