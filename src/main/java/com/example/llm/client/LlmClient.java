package com.example.llm.client;

import java.util.List;
import java.util.Map;

import com.example.llm.model.JudgeResult;
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
            throw new RuntimeException("Ollama error: " + response.asString());
        }

        try {
            JsonNode root = MAPPER.readTree(response.asString());
            return root.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Parse error", e);
        }
    }

    public JudgeResult judge(String userPrompt, String assistantAnswer) {
        String judgePrompt = String.format(
            "Score this response 0.0-1.0. Output ONLY valid JSON: {\"score\":0.5,\"reasoning\":\"...\"}. \nPrompt: %s\nAnswer: %s", 
            userPrompt, assistantAnswer);
            
        String raw = generate("llama3", judgePrompt);
        
        try {
            String cleaned = raw.replaceAll("```json|```", "").trim();
            return MAPPER.readValue(cleaned, JudgeResult.class);
        } catch (Exception e) {
            return new JudgeResult(0.0, "Judge output not valid JSON. Raw: " + raw);
        }
    }
}