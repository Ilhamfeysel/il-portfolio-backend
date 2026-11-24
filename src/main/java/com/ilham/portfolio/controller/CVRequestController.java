
package com.ilham.portfolio.controller;

import com.ilham.portfolio.dto.CVRequest;
import com.ilham.portfolio.dto.CVRequestResponse;
import com.ilham.portfolio.ratelimit.CVRateLimiter;
import com.ilham.portfolio.service.CVDownloadTokenService;
import com.ilham.portfolio.service.CVRequestService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "http://localhost:5173")
public class CVRequestController {

    private final CVRequestService cvRequestService;
    private final CVDownloadTokenService tokenService;
    private final CVRateLimiter cvRateLimiter;

    public CVRequestController(
            CVRequestService cvRequestService,
            CVDownloadTokenService tokenService,
            CVRateLimiter cvRateLimiter) {

        this.cvRequestService = cvRequestService;
        this.tokenService = tokenService;
        this.cvRateLimiter = cvRateLimiter;
    }

    @PostMapping("/request")
    public ResponseEntity<CVRequestResponse> requestCV(
            @Valid @RequestBody CVRequest request,
            HttpServletRequest httpRequest) {

        // Identify the visitor by IP address
        String visitorId = httpRequest.getRemoteAddr();

        // Check rate limit before sending an email
        cvRateLimiter.checkLimit(visitorId);

        // Notify portfolio owner
        cvRequestService.processRequest(request);

        // Generate a single-use download token
        String token = tokenService.generateToken();

        return ResponseEntity.ok(
                new CVRequestResponse(
                        "Thanks for your interest in my work!",
                        token
                )
        );
    }
}








































