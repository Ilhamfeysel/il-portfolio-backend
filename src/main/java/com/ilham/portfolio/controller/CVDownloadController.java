package com.ilham.portfolio.controller;

import com.ilham.portfolio.service.CVDownloadTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "${portfolio.cors.allowed-origin}")
public class CVDownloadController {
    @Value("${portfolio.cv.file-path}")
    private String cvFilePath;

    private final CVDownloadTokenService tokenService;

    public CVDownloadController(
            CVDownloadTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadCV(
            @RequestParam String token) {

        Resource cv = new FileSystemResource(cvFilePath);

        if (!cv.exists() || !cv.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        if (!tokenService.consumeToken(token)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Ilham_Feysel_CV.pdf\""
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                )
                .body(cv);
    }
}
