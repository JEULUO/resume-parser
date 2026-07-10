package com.example.resumematcher.model;

import jakarta.validation.constraints.Size;

public record JobMatchRequest(
        @Size(min = 10, message = "岗位描述不少于 10 个字符")
        String jobDescription,
        String resumeId,
        String resumeText,
        ResumeKeyInfo keyInfo
) {
}
