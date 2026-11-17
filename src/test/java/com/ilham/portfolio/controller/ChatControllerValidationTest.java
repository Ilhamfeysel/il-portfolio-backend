package com.ilham.portfolio.controller;

import com.ilham.portfolio.dto.ChatRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatControllerValidationTest {

    @Test
    void shouldRejectMessagesLongerThan1000Characters() {

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            ChatRequest request = new ChatRequest(
                    "a".repeat(1001),
                    null
            );

            var violations = validator.validate(request);

            assertEquals(1, violations.size());

            assertEquals(
                    "Message cannot exceed 1000 characters",
                    violations.iterator().next().getMessage()
            );
        }
    }

    @Test
    void shouldAcceptMessagesWithinLimit() {

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            ChatRequest request = new ChatRequest(
                    "a".repeat(1000),
                    null
            );

            var violations = validator.validate(request);

            assertTrue(violations.isEmpty());
        }
    }
}

