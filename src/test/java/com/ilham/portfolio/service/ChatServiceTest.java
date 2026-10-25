
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Test
    void shouldReturnAiGeneratedReply() {

        AiService aiService = mock(AiService.class);

        when(aiService.generateReply("Hello"))
                .thenReturn("Hello! How can I help you?");

        ChatService chatService = new ChatService(aiService);

        ChatRequest request = new ChatRequest("Hello");

        ChatResponse response = chatService.sendMessage(request);

        assertEquals("Hello! How can I help you?", response.reply());

        verify(aiService).generateReply("Hello");
    }
}
