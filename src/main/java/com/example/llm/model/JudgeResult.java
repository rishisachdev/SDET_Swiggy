package com.example.llm.model;

public class JudgeResult {
    public double score;
    public String reasoning;

    public JudgeResult() {}

    public JudgeResult(double score, String reasoning) {
        this.score = score;
        this.reasoning = reasoning;
    }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}