
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private AiService aiService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        chatService = new ChatService(aiService);

        when(aiService.createMessage(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String role = invocation.getArgument(0);
                    String text = invocation.getArgument(1);

                    return Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", text))
                    );
                });
    }

    @Test
    void shouldGenerateConversationIdForNewChat() {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn("Hello!");

        ChatResponse response = chatService.sendMessage(
                new ChatRequest("Hi", null)
        );

        assertNotNull(response.conversationId());
        assertFalse(response.conversationId().isBlank());
        assertEquals("Hello!", response.reply());
    }

    @Test
    void shouldRememberPreviousMessagesInCorrectOrder() {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn(
                        "Ilham has built several projects.",
                        "The Healthcare Appointment App uses Spring Boot."
                );

        ChatResponse first = chatService.sendMessage(
                new ChatRequest("What projects has Ilham built?", null)
        );

        chatService.sendMessage(
                new ChatRequest(
                        "Which one uses Spring Boot?",
                        first.conversationId()
                )
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> captor =
                ArgumentCaptor.forClass((Class) List.class);

        verify(aiService, times(2))
                .generateReplyWithHistory(captor.capture());

        List<Map<String, Object>> secondRequest =
                captor.getAllValues().get(1);

        assertEquals(3, secondRequest.size());

        assertEquals("user", secondRequest.get(0).get("role"));
        assertEquals("model", secondRequest.get(1).get("role"));
        assertEquals("user", secondRequest.get(2).get("role"));

        assertEquals(
                "What projects has Ilham built?",
                getText(secondRequest.get(0))
        );

        assertEquals(
                "Ilham has built several projects.",
                getText(secondRequest.get(1))
        );

        assertEquals(
                "Which one uses Spring Boot?",
                getText(secondRequest.get(2))
        );
    }

    @Test
    void shouldKeepConversationsSeparate() {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn("Reply one", "Reply two");

        ChatResponse visitorOne = chatService.sendMessage(
                new ChatRequest("Hello from visitor one", null)
        );

        ChatResponse visitorTwo = chatService.sendMessage(
                new ChatRequest("Hello from visitor two", null)
        );

        assertNotEquals(
                visitorOne.conversationId(),
                visitorTwo.conversationId()
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> captor =
                ArgumentCaptor.forClass((Class) List.class);

        verify(aiService, times(2))
                .generateReplyWithHistory(captor.capture());

        List<List<Map<String, Object>>> requests =
                captor.getAllValues();

        assertEquals(1, requests.get(0).size());
        assertEquals(1, requests.get(1).size());

        assertEquals(
                "Hello from visitor one",
                getText(requests.get(0).get(0))
        );

        assertEquals(
                "Hello from visitor two",
                getText(requests.get(1).get(0))
        );
    }

    @Test
    void shouldStartNewConversationForUnknownId() {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn("Hello!");

        ChatResponse response = chatService.sendMessage(
                new ChatRequest("Hello", "unknown-id")
        );

        assertNotEquals("unknown-id", response.conversationId());
        assertNotNull(response.conversationId());
    }

    private String getText(Map<String, Object> message) {

        List<?> parts = (List<?>) message.get("parts");
        Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);

        return (String) firstPart.get("text");
    }
}

