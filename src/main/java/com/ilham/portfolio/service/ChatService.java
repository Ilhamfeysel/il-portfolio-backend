package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    public ChatResponse sendMessage(ChatRequest request) {
        String reply = "You asked: " + request.message();
        return new ChatResponse(reply);
    }
}

