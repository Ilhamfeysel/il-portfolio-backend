
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import com.ilham.portfolio.storage.ConversationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        ConversationStore conversationStore =
                new ConversationStore();

        chatService = new ChatService(aiService, conversationStore);

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


    @Test
    void shouldHandleConcurrentRequestsToSameConversation() throws Exception {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn("AI response");

        // Create an initial conversation
        ChatResponse initial = chatService.sendMessage(
                new ChatRequest("Hello", null)
        );

        String conversationId = initial.conversationId();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<ChatResponse> firstRequest = () ->
                    chatService.sendMessage(
                            new ChatRequest("First message", conversationId)
                    );

            Callable<ChatResponse> secondRequest = () ->
                    chatService.sendMessage(
                            new ChatRequest("Second message", conversationId)
                    );

            List<Future<ChatResponse>> results =
                    executor.invokeAll(
                            List.of(firstRequest, secondRequest)
                    );

            for (Future<ChatResponse> result : results) {
                assertEquals(
                        conversationId,
                        result.get().conversationId()
                );
            }

        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    void shouldPreserveHistoryAfterConcurrentRequests() throws Exception {

        when(aiService.generateReplyWithHistory(anyList()))
                .thenReturn("AI response");

        ChatResponse initial = chatService.sendMessage(
                new ChatRequest("Hello", null)
        );

        String conversationId = initial.conversationId();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<ChatResponse> first = () ->
                    chatService.sendMessage(
                            new ChatRequest("First message", conversationId)
                    );

            Callable<ChatResponse> second = () ->
                    chatService.sendMessage(
                            new ChatRequest("Second message", conversationId)
                    );

            List<Future<ChatResponse>> results =
                    executor.invokeAll(List.of(first, second));

            for (Future<ChatResponse> result : results) {
                assertEquals(conversationId, result.get().conversationId());
            }

        } finally {
            executor.shutdownNow();
        }

        // Send another message to inspect the accumulated history
        chatService.sendMessage(
                new ChatRequest("Final message", conversationId)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(aiService, times(4))
                .generateReplyWithHistory(captor.capture());

        List<Map<String, Object>> finalHistory =
                captor.getAllValues().get(3);

        List<String> userMessages = finalHistory.stream()
                .filter(message -> "user".equals(message.get("role")))
                .map(message -> {
                    List<Map<String, Object>> parts =
                            (List<Map<String, Object>>) message.get("parts");

                    return (String) parts.get(0).get("text");
                })
                .toList();

        assertTrue(userMessages.contains("Hello"));
        assertTrue(userMessages.contains("First message"));
        assertTrue(userMessages.contains("Second message"));
        assertTrue(userMessages.contains("Final message"));
    }




}

