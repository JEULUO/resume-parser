package com.example.resumematcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import com.example.resumematcher.model.PdfParseResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PdfParserServiceTest {
    @Test
    void extractsTextFromMultiPagePdf() throws Exception {
        byte[] pdf = createPdf();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf);

        PdfParseResult result = new PdfParserService().parse(file);

        assertThat(result.pages()).isEqualTo(2);
        assertThat(result.text()).contains("Java backend engineer");
        assertThat(result.text()).contains("Spring Boot Redis MySQL");
    }

    private byte[] createPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            addPage(document, "Java backend engineer");
            addPage(document, "Spring Boot Redis MySQL");
            document.save(output);
            return output.toByteArray();
        }
    }

    private void addPage(PDDocument document, String text) throws Exception {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
            content.newLineAtOffset(72, 720);
            content.showText(text);
            content.endText();
        }
    }
}
