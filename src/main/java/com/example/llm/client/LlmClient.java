package com.example.llm.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;


public class LlmClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    

    private static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";

    public LlmClient() {}


    public String generate(String model, String prompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", false
        );

        var response = given()
                .header("Authorization", "Bearer ollama")
                .contentType(ContentType.JSON)
                .body(body)
                .post(OLLAMA_URL);

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.statusCode() + " - " + response.asString());
        }

        try {
            JsonNode root = MAPPER.readTree(response.asString());
            return root.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response JSON", e);
        }
    }


    public String generateWithRetry(String model, String prompt, int maxRetries) throws InterruptedException {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return generate(model, prompt);
            } catch (Exception e) {
                attempt++;
                lastException = e;
                if (attempt >= maxRetries) break;
                
                System.out.println(String.format("DEBUG: Attempt %d failed. Retrying in 1s... (%s)", 
                    attempt, e.getMessage()));
                Thread.sleep(1000); 
            }
        }
        throw new RuntimeException("All retry attempts failed. Last error: " + 
            (lastException != null ? lastException.getMessage() : "Unknown"), lastException);
    }
}