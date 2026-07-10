package com.example.resumematcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.resumematcher.config.AppProperties;
import com.example.resumematcher.model.JobMatchResponse;
import com.example.resumematcher.model.ResumeKeyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MatchingServiceTest {
    @Test
    void scoresResumeAgainstJobDescription() {
        OpenAiService openAiService = new OpenAiService(new AppProperties("*", "", "gpt-4.1-mini"), new ObjectMapper());
        MatchingService service = new MatchingService(new KeywordService(), openAiService);
        ResumeKeyInfo info = new ResumeKeyInfo();
        info.setYearsOfExperience("3年");
        info.setSkills(List.of("Java", "Spring Boot", "Redis", "MySQL"));

        JobMatchResponse response = service.match(
                "demo",
                "Java Spring Boot Redis MySQL 3年工作经验",
                info,
                "招聘 Java 后端工程师，3年以上经验，熟悉 Spring Boot、Redis、MySQL。"
        );

        assertThat(response.score()).isGreaterThan(60);
        assertThat(response.detail().matchedKeywords()).contains("Java", "Redis", "MySQL");
    }
}
