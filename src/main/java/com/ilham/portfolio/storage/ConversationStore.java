package com.ilham.portfolio.storage;

import com.ilham.portfolio.exception.ConversationCapacityException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationStore {

    private static final Duration EXPIRATION =
            Duration.ofMinutes(30);
    private static final int MAX_CONVERSATIONS = 100;

    private final Clock clock;

    private final Map<String, Conversation> conversations =
            new ConcurrentHashMap<>();

    public ConversationStore() {
        this(Clock.systemUTC());
    }

    public ConversationStore(Clock clock) {
        this.clock = clock;
    }

    public Conversation get(String conversationId) {

        Conversation conversation =
                conversations.get(conversationId);

        if (conversation == null) {
            return null;
        }

        if (conversation.isExpired(clock)) {
            conversations.remove(conversationId, conversation);
            return null;
        }

        return conversation;
    }

    public boolean isFull() {

        removeExpired();

        return conversations.size() >= MAX_CONVERSATIONS;
    }


    public synchronized void save(
            String conversationId,
            Conversation conversation) {

        // Remove expired conversations before checking capacity
        removeExpired();

        // Updating an existing conversation does not use a new slot
        if (!conversations.containsKey(conversationId)
                && conversations.size() >= MAX_CONVERSATIONS) {

            throw new ConversationCapacityException(
                    "The chat service is currently busy. Please try again later."
            );
        }

        conversations.put(conversationId, conversation);
    }


    @Scheduled(fixedDelay = 300_000)
    public void removeExpired() {
        conversations.entrySet().removeIf(
                entry -> entry.getValue().isExpired(clock)
        );
    }

    public Conversation createConversation() {
        return new Conversation(
                new java.util.ArrayList<>(),
                Instant.now(clock)
        );
    }

    public static class Conversation {

        private final List<Map<String, Object>> messages;
        private Instant lastActivity;

        public Conversation(
                List<Map<String, Object>> messages,
                Instant lastActivity) {

            this.messages = messages;
            this.lastActivity = lastActivity;
        }

        public List<Map<String, Object>> getMessages() {
            return messages;
        }

        public void updateActivity(Clock clock) {
            lastActivity = Instant.now(clock);
        }

        public boolean isExpired(Clock clock) {
            return !Instant.now(clock).isBefore(
                    lastActivity.plus(EXPIRATION)
            );
        }
    }

    public void updateActivity(Conversation conversation) {
        conversation.updateActivity(clock);
    }
}

