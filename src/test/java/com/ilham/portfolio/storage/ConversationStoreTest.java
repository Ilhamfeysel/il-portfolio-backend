
package com.ilham.portfolio.storage;

import com.ilham.portfolio.exception.ConversationCapacityException;
import com.ilham.portfolio.storage.ConversationStore.Conversation;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    void shouldRejectNewConversationWhenStoreIsFull() {

        Clock clock = Clock.fixed(
                START_TIME,
                ZoneOffset.UTC
        );

        ConversationStore store = new ConversationStore(clock);

        // Create 100 conversations
        for (int i = 0; i < 100; i++) {
            store.save(
                    "conversation-" + i,
                    store.createConversation()
            );
        }

        // The 101st conversation should be rejected
        assertThrows(
                ConversationCapacityException.class,
                () -> store.save(
                        "conversation-101",
                        store.createConversation()
                )
        );
    }


    @Test
    void shouldAllowExistingConversationWhenStoreIsFull() {

        Clock clock = Clock.fixed(
                START_TIME,
                ZoneOffset.UTC
        );

        ConversationStore store = new ConversationStore(clock);

        // Fill the store with 100 conversations
        for (int i = 0; i < 100; i++) {
            store.save(
                    "conversation-" + i,
                    store.createConversation()
            );
        }

        // Retrieve an existing conversation
        Conversation existing =
                store.get("conversation-0");

        assertNotNull(existing);

        // Updating an existing conversation should be allowed
        assertDoesNotThrow(() ->
                store.save("conversation-0", existing)
        );

        // The conversation should still be available
        assertSame(
                existing,
                store.get("conversation-0")
        );

        // But a new conversation should still be rejected
        assertThrows(
                ConversationCapacityException.class,
                () -> store.save(
                        "conversation-101",
                        store.createConversation()
                )
        );
    }


    @Test
    void shouldNotRemoveConversationWhileRequestIsActive() {

        Clock clock = Clock.fixed(
                START_TIME,
                ZoneOffset.UTC
        );

        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();

        store.save("active-conversation", conversation);

        // Simulate a request starting
        Conversation acquired = store.acquire("active-conversation");

        assertNotNull(acquired);

        // Simulate the conversation becoming old
        Clock laterClock = Clock.offset(
                clock,
                Duration.ofMinutes(31)
        );

        // Cleanup must not remove an active conversation
        assertFalse(acquired.isExpired(laterClock));

        store.removeExpired();

        assertSame(
                conversation,
                store.get("active-conversation")
        );

        // Finish the request
        acquired.endRequest();
    }



    @Test
    void shouldKeepActiveConversationAfterExpirationTime() {

        AtomicReference<Instant> currentTime =
                new AtomicReference<>(START_TIME);

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return Clock.fixed(currentTime.get(), zone);
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };

        ConversationStore store = new ConversationStore(clock);

        Conversation conversation = store.createConversation();
        store.save("conversation-1", conversation);

        // Start a request
        Conversation acquired = store.acquire("conversation-1");
        assertNotNull(acquired);

        // Advance the store's clock by 31 minutes
        currentTime.set(START_TIME.plus(Duration.ofMinutes(31)));

        // Cleanup should preserve the active conversation
        store.removeExpired();

        assertSame(conversation, store.get("conversation-1"));

        // Finish the request and update activity
        store.updateActivity(acquired);
        acquired.endRequest();

        // Conversation should still be available
        store.removeExpired();

        assertSame(conversation, store.get("conversation-1"));
    }




}


