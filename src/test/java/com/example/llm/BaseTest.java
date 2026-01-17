package com.example.llm;

import org.junit.jupiter.api.BeforeAll;

import com.example.llm.client.LlmClient;

public class BaseTest {
    protected static LlmClient client;

    @BeforeAll
    static void setup() {
        client = new LlmClient();
    }
}