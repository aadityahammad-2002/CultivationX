package com.cultivationx.rise.parser;

import com.cultivationx.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Component
public class ResumeParser {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private final Tika tika = new Tika();

    public String validateAndDetectType(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("File is empty");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw AppException.fileTooLarge(file.getSize() / (1024 * 1024));
        }

        try {
            String detectedType = tika.detect(file.getInputStream(), file.getOriginalFilename());
            if (!ALLOWED_TYPES.contains(detectedType)) {
                throw AppException.invalidFileType(detectedType);
            }
            return detectedType;
        } catch (IOException e) {
            throw AppException.resumeParseError("Cannot read file: " + e.getMessage());
        }
    }

    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BodyContentHandler handler = new BodyContentHandler(1024 * 1024); // 1MB text limit
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();

            metadata.set(Metadata.CONTENT_TYPE, file.getContentType());

            try {
                parser.parse(inputStream, handler, metadata, new org.apache.tika.parser.ParseContext());
            } catch (SAXException | TikaException e) {
                throw AppException.resumeParseError("Tika parsing failed: " + e.getMessage());
            }

            String text = handler.toString().trim();
            if (text.isBlank()) {
                throw AppException.resumeParseError("Could not extract text from file. The file may be scanned or image-based.");
            }

            log.info("Extracted {} characters from resume", text.length());
            return text;

        } catch (IOException e) {
            throw AppException.resumeParseError("IO error: " + e.getMessage());
        }
    }
}