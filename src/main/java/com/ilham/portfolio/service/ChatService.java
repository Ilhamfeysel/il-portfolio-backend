
package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.ChatRequest;
import com.ilham.portfolio.dto.ChatResponse;
import com.ilham.portfolio.storage.ConversationStore;
import com.ilham.portfolio.storage.ConversationStore.Conversation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiService aiService;
    private final ConversationStore conversationStore;

    public ChatService(
            AiService aiService,
            ConversationStore conversationStore) {

        this.aiService = aiService;
        this.conversationStore = conversationStore;
    }

    public ChatResponse sendMessage(ChatRequest request) {

        String conversationId = request.conversationId();

        Conversation conversation = conversationId == null
                ? null
                : conversationStore.get(conversationId);

        // Create a new conversation when the ID is missing or expired
        if (conversation == null) {

            conversationId = UUID.randomUUID().toString();

            conversation = conversationStore.createConversation();

            conversationStore.save(conversationId, conversation);
        }

        synchronized (conversation) {

            List<Map<String, Object>> history =
                    conversation.getMessages();

            // Prepare the messages for Gemini
            List<Map<String, Object>> messages =
                    new ArrayList<>(history);

            messages.add(
                    aiService.createMessage("user", request.message())
            );

            // Ask Gemini for a reply
            String reply =
                    aiService.generateReplyWithHistory(messages);

            // Save the successful exchange
            history.add(
                    aiService.createMessage("user", request.message())
            );

            history.add(
                    aiService.createMessage("model", reply)
            );

            // Keep only the latest 10 messages
            while (history.size() > MAX_HISTORY_MESSAGES) {
                history.remove(0);
            }

            // Reset inactivity timer
            conversationStore.updateActivity(conversation);

            return new ChatResponse(reply, conversationId);
        }
    }
}







