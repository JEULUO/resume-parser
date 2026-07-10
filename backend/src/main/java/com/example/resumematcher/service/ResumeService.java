package com.example.resumematcher.service;

import com.example.resumematcher.exception.BadRequestException;
import com.example.resumematcher.model.CleanTextResult;
import com.example.resumematcher.model.JobMatchRequest;
import com.example.resumematcher.model.JobMatchResponse;
import com.example.resumematcher.model.PdfParseResult;
import com.example.resumematcher.model.ResumeKeyInfo;
import com.example.resumematcher.model.ResumeParseResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {
    private final PdfParserService pdfParserService;
    private final TextCleanerService textCleanerService;
    private final ResumeExtractionService resumeExtractionService;
    private final MatchingService matchingService;
    private final CacheService cacheService;
    private final HashService hashService;

    public ResumeService(
            PdfParserService pdfParserService,
            TextCleanerService textCleanerService,
            ResumeExtractionService resumeExtractionService,
            MatchingService matchingService,
            CacheService cacheService,
            HashService hashService
    ) {
        this.pdfParserService = pdfParserService;
        this.textCleanerService = textCleanerService;
        this.resumeExtractionService = resumeExtractionService;
        this.matchingService = matchingService;
        this.cacheService = cacheService;
        this.hashService = hashService;
    }

    public ResumeParseResponse parseResume(MultipartFile file) {
        byte[] bytes = readBytes(file);
        String resumeId = hashService.sha256(bytes);
        String cacheKey = "resume:" + resumeId;
        return cacheService.get(cacheKey, ResumeParseResponse.class)
                .map(response -> response.withCacheHit(true))
                .orElseGet(() -> parseAndCache(file, resumeId, cacheKey));
    }

    public JobMatchResponse match(JobMatchRequest request) {
        if (request.jobDescription() == null || request.jobDescription().trim().length() < 10) {
            throw new BadRequestException("岗位描述不少于 10 个字符。");
        }
        ResolvedResume resolved = resolveResume(request);
        String cacheKey = "match:" + hashService.sha256((request.resumeId() == null ? resolved.text() : request.resumeId()) + "|" + request.jobDescription());
        return cacheService.get(cacheKey, JobMatchResponse.class)
                .map(response -> response.withCacheHit(true))
                .orElseGet(() -> {
                    JobMatchResponse response = matchingService.match(request.resumeId(), resolved.text(), resolved.keyInfo(), request.jobDescription());
                    cacheService.set(cacheKey, response);
                    return response;
                });
    }

    private ResumeParseResponse parseAndCache(MultipartFile file, String resumeId, String cacheKey) {
        PdfParseResult pdf = pdfParserService.parse(file);
        CleanTextResult clean = textCleanerService.clean(pdf.text());
        ResumeKeyInfo keyInfo = resumeExtractionService.extract(clean.cleanedText());
        ResumeParseResponse response = new ResumeParseResponse(
                resumeId,
                file.getOriginalFilename(),
                pdf.pages(),
                clean.cleanedText(),
                clean.paragraphs(),
                keyInfo,
                false
        );
        cacheService.set(cacheKey, response);
        return response;
    }

    private ResolvedResume resolveResume(JobMatchRequest request) {
        if (request.resumeId() != null && !request.resumeId().isBlank()) {
            var cached = cacheService.get("resume:" + request.resumeId(), ResumeParseResponse.class);
            if (cached.isPresent()) {
                ResumeParseResponse response = cached.get();
                return new ResolvedResume(response.cleanedText(), response.keyInfo());
            }
        }
        if (request.resumeText() != null && !request.resumeText().isBlank()) {
            ResumeKeyInfo keyInfo = request.keyInfo() == null ? resumeExtractionService.extract(request.resumeText()) : request.keyInfo();
            return new ResolvedResume(request.resumeText(), keyInfo);
        }
        throw new BadRequestException("请提供 resume_id，或直接提供 resume_text。");
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BadRequestException("无法读取上传文件。");
        }
    }

    private record ResolvedResume(String text, ResumeKeyInfo keyInfo) {
    }
}
