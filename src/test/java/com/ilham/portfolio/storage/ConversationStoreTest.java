
package com.ilham.portfolio.storage;

import com.ilham.portfolio.storage.ConversationStore.Conversation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class ConversationStoreTest {

    private static final Instant START_TIME =
            Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void shouldSaveAndRetrieveConversation() {

        Clock clock = Clock.fixed(START_TIME, ZoneOffset.UTC);
        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();
        store.save("test-123", conversation);

        assertSame(conversation, store.get("test-123"));
    }

    @Test
    void shouldReturnNullForUnknownConversation() {

        Clock clock = Clock.fixed(START_TIME, ZoneOffset.UTC);
        ConversationStore store = new ConversationStore(clock);

        assertNull(store.get("unknown-id"));
    }

    @Test
    void shouldKeepConversationBefore30Minutes() {

        Clock clock = Clock.fixed(START_TIME, ZoneOffset.UTC);
        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();
        store.save("test-123", conversation);

        Clock laterClock = Clock.fixed(
                START_TIME.plusSeconds(29 * 60),
                ZoneOffset.UTC
        );

        assertFalse(conversation.isExpired(laterClock));
    }

    @Test
    void shouldExpireConversationAt30Minutes() {

        Clock clock = Clock.fixed(START_TIME, ZoneOffset.UTC);
        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();
        store.save("test-123", conversation);

        Clock laterClock = Clock.fixed(
                START_TIME.plusSeconds(30 * 60),
                ZoneOffset.UTC
        );

        assertTrue(conversation.isExpired(laterClock));
    }

    @Test
    void shouldRemainExpiredAfter31Minutes() {

        Clock clock = Clock.fixed(START_TIME, ZoneOffset.UTC);
        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();

        Clock laterClock = Clock.fixed(
                START_TIME.plusSeconds(31 * 60),
                ZoneOffset.UTC
        );

        assertTrue(conversation.isExpired(laterClock));
    }

    @Test
    void shouldResetExpirationAfterActivity() {

        Clock initialClock =
                Clock.fixed(START_TIME, ZoneOffset.UTC);

        ConversationStore store = new ConversationStore(initialClock);
        Conversation conversation = store.createConversation();

        Clock activityClock = Clock.fixed(
                START_TIME.plusSeconds(20 * 60),
                ZoneOffset.UTC
        );

        conversation.updateActivity(activityClock);

        Clock checkClock = Clock.fixed(
                START_TIME.plusSeconds(40 * 60),
                ZoneOffset.UTC
        );

        assertFalse(conversation.isExpired(checkClock));
    }


    @Test
    void shouldRemoveExpiredConversationFromStore() {

        Clock clock = Clock.fixed(
                START_TIME,
                ZoneOffset.UTC
        );

        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();
        store.save("test-123", conversation);

        // Confirm the conversation exists
        assertNotNull(store.get("test-123"));

        // Simulate a conversation whose last activity
        // was more than 30 minutes ago
        Clock oldClock = Clock.fixed(
                START_TIME.minusSeconds(31 * 60),
                ZoneOffset.UTC
        );

        conversation.updateActivity(oldClock);

        // Remove expired conversations
        store.removeExpired();

        // The expired conversation should no longer exist
        assertNull(store.get("test-123"));
    }

}


