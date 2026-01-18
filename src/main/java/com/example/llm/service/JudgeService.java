package com.example.llm.service;

import com.example.llm.client.LlmClient;
import com.example.llm.model.JudgeResult;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JudgeService {
    private final LlmClient client;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JUDGE_MODEL = "llama3.2:1b";

    public JudgeService(LlmClient client) {
        this.client = client;
    }

    public JudgeResult judge(String userPrompt, String assistantAnswer) {
        
        String judgePrompt = """
            ### Task: Rate the following response quality from 0.0 (wrong) to 1.0 (perfect).
            ### User Prompt: %s
            ### Assistant Answer: %s
            ### Requirement: Return ONLY JSON format: {"score": 0.9, "reasoning": "explanation"}
            """.formatted(userPrompt, assistantAnswer);
            
        try {
            
            String raw = client.generate(JUDGE_MODEL, judgePrompt);
            
            int start = raw.indexOf("{");
            int end = raw.lastIndexOf("}");
            if (start != -1 && end != -1) {
                String cleaned = raw.substring(start, end + 1);
                return MAPPER.readValue(cleaned, JudgeResult.class);
            }
            
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d\\.\\d+)").matcher(raw);
            if (m.find()) {
                return new JudgeResult(Double.parseDouble(m.group(1)), "Extracted via regex");
            }
            
            throw new RuntimeException("No valid JSON found");
        } catch (Exception e) {
            return new JudgeResult(0.5, "Judge Service Error: " + e.getMessage());
        }
    }
}