package com.example.llm;

import org.junit.jupiter.api.BeforeAll;

import com.example.llm.client.LlmClient;
import com.example.llm.service.JudgeService;

public class BaseTest {
    protected static LlmClient client;
    protected static JudgeService judgeService;

    @BeforeAll
    static void setup() {
        client = new LlmClient();
        judgeService = new JudgeService(client);
    }
}