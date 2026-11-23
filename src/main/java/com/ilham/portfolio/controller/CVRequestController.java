

package com.ilham.portfolio.controller;

import com.ilham.portfolio.dto.CVRequest;
import com.ilham.portfolio.dto.CVRequestResponse;
import com.ilham.portfolio.service.CVDownloadTokenService;
import com.ilham.portfolio.service.CVRequestService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "http://localhost:5173")
public class CVRequestController {

    private final CVRequestService cvRequestService;
    private final CVDownloadTokenService tokenService;

    public CVRequestController(
            CVRequestService cvRequestService,
            CVDownloadTokenService tokenService) {

        this.cvRequestService = cvRequestService;
        this.tokenService = tokenService;
    }

    @PostMapping("/request")
    public ResponseEntity<CVRequestResponse> requestCV(
            @Valid @RequestBody CVRequest request) {

        // Notify portfolio owner first
        cvRequestService.processRequest(request);

        // Generate token only after notification succeeds
        String token = tokenService.generateToken();

        return ResponseEntity.ok(
                new CVRequestResponse(
                        "Thanks for your interest in my work!",
                        token
                )
        );
    }
}







































