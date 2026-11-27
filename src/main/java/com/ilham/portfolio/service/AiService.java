package com.ilham.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String portfolioContext;

    public AiService(@Value("${gemini.api.key}") String apiKey)
            throws IOException {

        this.apiKey = apiKey;


        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(requestFactory)
                .build();



        ClassPathResource resource =
                new ClassPathResource("portfolio-context.txt");

        try (InputStream inputStream = resource.getInputStream()) {
            this.portfolioContext = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    // Keep the original method for compatibility
    public String generateReply(String message) {

        List<Map<String, Object>> messages = List.of(
                createMessage("user", message)
        );

        return generateReplyWithHistory(messages);
    }

    // Generate a reply using the conversation history
    public String generateReplyWithHistory(
            List<Map<String, Object>> messages) {

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", portfolioContext)
                        )
                ),
                "contents", messages
        );

        Map<?, ?> response = restClient.post()
                .uri("/v1beta/models/gemini-3.5-flash-lite:generateContent")
                .header("x-goog-api-key", apiKey)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Empty response from Gemini"
            );
        }

        List<?> candidates =
                (List<?>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned no candidates"
            );
        }

        Map<?, ?> candidate =
                (Map<?, ?>) candidates.get(0);

        Map<?, ?> content =
                (Map<?, ?>) candidate.get("content");

        List<?> parts =
                (List<?>) content.get("parts");

        Map<?, ?> firstPart =
                (Map<?, ?>) parts.get(0);

        return (String) firstPart.get("text");
    }

    // Convert a message into Gemini's expected format
    public Map<String, Object> createMessage(
            String role,
            String text) {

        return Map.of(
                "role", role,
                "parts", List.of(
                        Map.of("text", text)
                )
        );
    }
}


