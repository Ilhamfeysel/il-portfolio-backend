
package com.ilham.portfolio.controller;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import com.ilham.portfolio.ratelimit.RateLimiter;
import com.ilham.portfolio.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final ChatService chatService;


    private final RateLimiter rateLimiter;

    public ChatController(
            ChatService chatService,
            RateLimiter rateLimiter) {

        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }



    @PostMapping
    public ChatResponse sendMessage(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String visitorId = httpRequest.getRemoteAddr();

        rateLimiter.checkLimit(visitorId);

        return chatService.sendMessage(request);
    }





}


