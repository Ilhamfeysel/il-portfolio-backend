
package com.ilham.portfolio.controller;

import com.ilham.portfolio.service.CVDownloadTokenService;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "http://localhost:5173")
public class CVDownloadController {

    private final CVDownloadTokenService tokenService;

    public CVDownloadController(
            CVDownloadTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadCV(
            @RequestParam String token) {

        Resource cv = new ClassPathResource(
                "cv/Ilham_Feysel_CV.pdf"
        );

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
