package com.example.resumematcher.service;

import java.io.IOException;

import com.example.resumematcher.exception.BadRequestException;
import com.example.resumematcher.model.PdfParseResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfParserService {
    public PdfParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("上传文件为空。");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            throw new BadRequestException("仅支持上传 PDF 格式简历。");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new BadRequestException("暂不支持加密 PDF，请先移除密码后再上传。");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BadRequestException("未能从 PDF 中提取文本，扫描件请先进行 OCR。");
            }
            return new PdfParseResult(text, document.getNumberOfPages());
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("无法读取 PDF 文件，请确认文件未损坏且不是加密 PDF。");
        }
    }
}
