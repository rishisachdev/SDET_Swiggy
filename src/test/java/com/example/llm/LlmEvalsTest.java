package com.example.llm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.llm.model.EvalCase;
import com.example.llm.model.JudgeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class LlmEvalsTest extends BaseTest {

    private static final String MODEL_NAME = "llama3.2:1b";

    static Stream<EvalCase> evalCases() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = LlmEvalsTest.class.getClassLoader().getResourceAsStream("evals.json");
        if (is == null) {
            throw new RuntimeException("Could not find evals.json in resources");
        }
        List<EvalCase> list = mapper.readValue(is, new TypeReference<>() {});
        return list.stream();
    }

    @ParameterizedTest(name = "Evaluation Case: {0}")
    @MethodSource("evalCases")
    void runEval(EvalCase eval) throws InterruptedException {
        List<Double> scores = new ArrayList<>();
        int passedRuns = 0;
        List<String> errorLogs = new ArrayList<>();

        System.out.println("\n>>> Starting Non-Deterministic Evaluation: " + eval.id);

        for (int run = 1; run <= eval.judge.runs; run++) {

            String structuredPrompt = "Instruction: Provide a factual answer. \nQuestion: " + eval.prompt;
            
            String response = client.generateWithRetry(MODEL_NAME, structuredPrompt, 3);
            
            System.out.println(String.format("[%s] Run %d/%d Response: %s", 
                eval.id, run, eval.judge.runs, response));

            
            boolean lengthValid = response.length() >= eval.validation.minLengthChars;

            boolean keywordsValid = eval.validation.mustContainAny.stream()
                .allMatch(group -> group.stream().anyMatch(word -> response.toLowerCase().contains(word.toLowerCase())));

            boolean safetyValid = eval.validation.mustNotContain.stream()
                .noneMatch(forbidden -> response.toLowerCase().contains(forbidden.toLowerCase()));

            
            JudgeResult judgeResult = judgeService.judge(eval.prompt, response);
            scores.add(judgeResult.score);

            if (lengthValid && keywordsValid && safetyValid && judgeResult.score >= eval.judge.minWorstScore) {
                passedRuns++;
            } else {
                errorLogs.add(String.format("Run %d failed: Length(%b), Keywords(%b), Safety(%b), Score(%.2f)", 
                    run, lengthValid, keywordsValid, safetyValid, judgeResult.score));
            }
        }

        
        double meanScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double passRate = (double) passedRuns / eval.judge.runs;

        System.out.println(String.format("Final Metrics for %s -> Mean: %.2f, Pass Rate: %.2f", 
            eval.id, meanScore, passRate));
        
        if (passRate < eval.judge.minPassRate) {
            throw new RuntimeException(String.format(
                "[GATE FAILURE] %s: Pass Rate %.2f is below threshold %.2f. Issues: %s", 
                eval.id, passRate, eval.judge.minPassRate, errorLogs));
        }

        if (meanScore < eval.judge.minMeanScore) {
            throw new RuntimeException(String.format(
                "[GATE FAILURE] %s: Mean Semantic Score %.2f is below threshold %.2f", 
                eval.id, meanScore, eval.judge.minMeanScore));
        }
    }
}