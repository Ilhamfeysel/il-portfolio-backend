
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceTest {

    private final ChatService chatService = new ChatService();

    @Test
    void shouldReturnReplyForValidMessage() {
        ChatRequest request = new ChatRequest("Hello");

        ChatResponse response = chatService.sendMessage(request);

        assertEquals("You asked: Hello", response.reply());
    }
}
