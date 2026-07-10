package com.example.resumematcher.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.resumematcher.model.ResumeKeyInfo;
import org.springframework.stereotype.Service;

@Service
public class ResumeExtractionService {
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?86[- ]?)?1[3-9]\\d{9}(?!\\d)");
    private static final Pattern SALARY = Pattern.compile("(?:期望薪资|薪资要求|期望月薪)[:： ]*([0-9kK万千\\-~至以上/每月]+)");
    private static final Pattern YEARS = Pattern.compile("(\\d{1,2})\\s*年(?:以上)?(?:工作|开发|项目|相关)?经验");
    private static final List<String> COMMON_SKILLS = List.of(
            "Python", "Java", "JavaScript", "TypeScript", "React", "Vue", "FastAPI", "Spring", "Spring Boot",
            "MySQL", "PostgreSQL", "Redis", "Docker", "Kubernetes", "Linux", "NLP", "机器学习", "深度学习",
            "大模型", "数据分析", "爬虫", "Git"
    );

    private final OpenAiService openAiService;

    public ResumeExtractionService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public ResumeKeyInfo extract(String text) {
        ResumeKeyInfo ruleInfo = extractWithRules(text);
        return openAiService.extractResumeInfo(text)
                .map(aiInfo -> enrich(aiInfo, ruleInfo))
                .orElse(ruleInfo);
    }

    ResumeKeyInfo extractWithRules(String text) {
        ResumeKeyInfo info = new ResumeKeyInfo();
        info.setName(guessName(text));
        info.setPhone(firstMatch(PHONE, text));
        info.setEmail(firstMatch(EMAIL, text));
        info.setAddress(firstGroup(Pattern.compile("(?:地址|现居地|所在地)[:： ]*([^\\n，,;；]{2,40})"), text));
        info.setJobIntention(firstGroup(Pattern.compile("(?:求职意向|应聘岗位|目标岗位)[:： ]*([^\\n，,;；]{2,50})"), text));
        info.setExpectedSalary(firstGroup(SALARY, text));

        String years = firstGroup(YEARS, text);
        info.setYearsOfExperience(years == null ? null : years + "年");
        info.setEducationBackground(findSections(text, List.of("教育经历", "教育背景", "学历", "Education")));
        info.setProjectExperiences(findSections(text, List.of("项目经历", "项目经验", "Projects")));
        info.setWorkExperiences(findSections(text, List.of("工作经历", "实习经历", "工作经验", "Experience")));
        info.setSkills(extractSkills(text));
        return info;
    }

    private ResumeKeyInfo enrich(ResumeKeyInfo aiInfo, ResumeKeyInfo fallback) {
        if (isBlank(aiInfo.getName())) aiInfo.setName(fallback.getName());
        if (isBlank(aiInfo.getPhone())) aiInfo.setPhone(fallback.getPhone());
        if (isBlank(aiInfo.getEmail())) aiInfo.setEmail(fallback.getEmail());
        if (isBlank(aiInfo.getAddress())) aiInfo.setAddress(fallback.getAddress());
        if (isBlank(aiInfo.getJobIntention())) aiInfo.setJobIntention(fallback.getJobIntention());
        if (isBlank(aiInfo.getExpectedSalary())) aiInfo.setExpectedSalary(fallback.getExpectedSalary());
        if (isBlank(aiInfo.getYearsOfExperience())) aiInfo.setYearsOfExperience(fallback.getYearsOfExperience());
        if (aiInfo.getEducationBackground().isEmpty()) aiInfo.setEducationBackground(fallback.getEducationBackground());
        if (aiInfo.getProjectExperiences().isEmpty()) aiInfo.setProjectExperiences(fallback.getProjectExperiences());
        if (aiInfo.getWorkExperiences().isEmpty()) aiInfo.setWorkExperiences(fallback.getWorkExperiences());
        if (aiInfo.getSkills().isEmpty()) aiInfo.setSkills(fallback.getSkills());
        return aiInfo;
    }

    private String guessName(String text) {
        String[] lines = text == null ? new String[0] : text.split("\\R");
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            String compact = lines[i].replaceAll("\\s+", "")
                    .replaceAll("(?i)(姓名|个人简历|简历|Resume|CV)[:：]?", "");
            if (compact.matches("[\\u3400-\\u9fff]{2,4}")) {
                return compact;
            }
        }
        return firstGroup(Pattern.compile("姓名[:： ]*([\\u3400-\\u9fff]{2,4})"), text);
    }

    private List<String> findSections(String text, List<String> headers) {
        String headerPattern = headers.stream().map(Pattern::quote).reduce((a, b) -> a + "|" + b).orElse("");
        Pattern pattern = Pattern.compile(
                "(?ims)^\\s*(?:" + headerPattern + ")[:：]?\\s*(.*?)(?=^\\s*(?:教育经历|教育背景|工作经历|实习经历|项目经历|项目经验|技能|自我评价|个人信息|求职意向)[:：]?\\s*|\\z)"
        );
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            String segment = matcher.group(1).replaceAll("\\s+", " ").replaceAll("^[\\s\\-•]+|[\\s\\-•]+$", "");
            if (segment.length() > 8) {
                results.add(segment.substring(0, Math.min(segment.length(), 500)));
            }
        }
        return dedupe(results).stream().limit(5).toList();
    }

    private List<String> extractSkills(String text) {
        List<String> found = new ArrayList<>();
        for (String skill : COMMON_SKILLS) {
            Pattern pattern = Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(skill) + "(?![A-Za-z])");
            if (pattern.matcher(text == null ? "" : text).find()) {
                found.add(skill);
            }
        }
        return dedupe(found);
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : null;
    }

    private String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private List<String> dedupe(List<String> items) {
        Set<String> seen = new LinkedHashSet<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                seen.add(item.trim());
            }
        }
        return new ArrayList<>(seen);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
