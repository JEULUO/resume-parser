package com.example.resumematcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.resumematcher.config.AppProperties;
import com.example.resumematcher.model.ResumeKeyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResumeExtractionServiceTest {
    @Test
    void extractsBasicInfoWithRules() {
        OpenAiService openAiService = new OpenAiService(new AppProperties("*", "", "gpt-4.1-mini"), new ObjectMapper());
        ResumeExtractionService service = new ResumeExtractionService(openAiService);

        ResumeKeyInfo info = service.extractWithRules("""
                毛骄
                电话：15523063767
                邮箱：maojiao@example.com
                地址：重庆
                求职意向：Java 后端开发
                3年工作经验
                技能：Java Spring Boot Redis MySQL Docker
                项目经历：AI 简历解析系统，负责 PDF 解析和岗位匹配。
                """);

        assertThat(info.getName()).isEqualTo("毛骄");
        assertThat(info.getPhone()).isEqualTo("15523063767");
        assertThat(info.getEmail()).isEqualTo("maojiao@example.com");
        assertThat(info.getYearsOfExperience()).isEqualTo("3年");
        assertThat(info.getSkills()).contains("Java", "Spring Boot", "Redis", "MySQL");
    }
}
