package com.ilham.portfolio.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturn503WhenGeminiFails() {

        // Arrange: simulate a Gemini API failure
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();

        RestClientException exception =
                new RestClientException("Gemini API failed");

        // Act: handle the exception
        ResponseEntity<Map<String, String>> response =
                handler.handleAiError(exception);

        // Assert: verify HTTP 503
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        // Assert: verify the friendly error message
        assertEquals(
                "The AI assistant is temporarily unavailable. Please try again later.",
                response.getBody().get("error")
        );
    }
}

