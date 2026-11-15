
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiService aiService;

    private final Map<String, List<Map<String, Object>>> conversations =
            new ConcurrentHashMap<>();

    public ChatService(AiService aiService) {
        this.aiService = aiService;
    }

    public ChatResponse sendMessage(ChatRequest request) {

        String conversationId = request.conversationId();

        // Create a new conversation if the ID is missing or unknown
        if (conversationId == null
                || !conversations.containsKey(conversationId)) {

            conversationId = UUID.randomUUID().toString();
            conversations.put(conversationId, new ArrayList<>());
        }

        List<Map<String, Object>> history =
                conversations.get(conversationId);

        synchronized (history) {

            // Prepare messages without changing stored history yet
            List<Map<String, Object>> messages =
                    new ArrayList<>(history);

            messages.add(
                    aiService.createMessage("user", request.message())
            );

            // Send conversation history to Gemini
            String reply =
                    aiService.generateReplyWithHistory(messages);

            // Save messages only after Gemini succeeds
            history.add(
                    aiService.createMessage("user", request.message())
            );

            history.add(
                    aiService.createMessage("model", reply)
            );

            // Keep only the latest messages
            while (history.size() > MAX_HISTORY_MESSAGES) {
                history.remove(0);
            }

            return new ChatResponse(reply, conversationId);
        }
    }
}






