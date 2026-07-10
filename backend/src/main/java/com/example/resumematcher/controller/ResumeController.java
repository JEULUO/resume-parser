package com.example.resumematcher.controller;

import java.util.Map;

import com.example.resumematcher.model.AnalyzeResponse;
import com.example.resumematcher.model.JobMatchRequest;
import com.example.resumematcher.model.JobMatchResponse;
import com.example.resumematcher.model.ResumeParseResponse;
import com.example.resumematcher.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResumeController {
    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "app", "AI Resume Parser");
    }

    @PostMapping("/api/resumes/upload")
    public ResumeParseResponse uploadResume(@RequestPart("file") MultipartFile file) {
        return resumeService.parseResume(file);
    }

    @PostMapping("/api/match")
    public JobMatchResponse match(@Valid @RequestBody JobMatchRequest request) {
        return resumeService.match(request);
    }

    @PostMapping("/api/analyze")
    public AnalyzeResponse analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "job_description", required = false) String jobDescription
    ) {
        ResumeParseResponse resume = resumeService.parseResume(file);
        JobMatchResponse match = null;
        if (jobDescription != null && !jobDescription.isBlank()) {
            match = resumeService.match(new JobMatchRequest(jobDescription, resume.resumeId(), null, null));
        }
        return new AnalyzeResponse(resume, match);
    }
}
