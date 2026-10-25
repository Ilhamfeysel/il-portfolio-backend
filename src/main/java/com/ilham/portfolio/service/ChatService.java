package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final AiService aiService;

    public ChatService(AiService aiService) {
        this.aiService = aiService;
    }

    public ChatResponse sendMessage(ChatRequest request) {
        String reply = aiService.generateReply(request.message());
        return new ChatResponse(reply);
    }
}




