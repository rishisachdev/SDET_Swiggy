package com.example.llm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("evalCases")
    void runEval(EvalCase eval) throws InterruptedException {
        List<Double> scores = new ArrayList<>();
        int passCount = 0;

        System.out.println("Starting Eval: " + eval.id);

        for (int run = 1; run <= eval.judge.runs; run++) {
            String response = generateWithRetry(eval.prompt, 3);
            
            System.out.println(String.format("[%s] Run %d/%d Response: %s", 
                eval.id, run, eval.judge.runs, response));

            Assertions.assertTrue(
                    response.length() >= eval.validation.minLengthChars,
                    String.format("[FAIL] %s (run %d): Response length %d < min %d", 
                        eval.id, run, response.length(), eval.validation.minLengthChars)
            );

            for (List<String> group : eval.validation.mustContainAny) {
                boolean foundInGroup = group.stream()
                        .anyMatch(word -> response.toLowerCase().contains(word.toLowerCase()));

                Assertions.assertTrue(
                        foundInGroup,
                        String.format("[FAIL] %s (run %d): Missing keywords from group %s", 
                            eval.id, run, group)
                );
            }

            for (String forbidden : eval.validation.mustNotContain) {
                Assertions.assertFalse(
                        response.toLowerCase().contains(forbidden.toLowerCase()),
                        String.format("[FAIL] %s (run %d): Found forbidden phrase: %s", 
                            eval.id, run, forbidden)
                );
            }

            JudgeResult judge = client.judge(eval.prompt, response);
            scores.add(judge.score);

            if (judge.score >= eval.judge.minWorstScore) {
                passCount++;
            }
            
            //Thread.sleep(2000); 
        }

        double meanScore = scores.stream().mapToDouble(s -> s).average().orElse(0.0);
        double worstScore = scores.stream().mapToDouble(s -> s).min().orElse(0.0);
        double passRate = (double) passCount / eval.judge.runs;

        System.out.println(String.format("Results for %s -> Mean: %.2f, Worst: %.2f, Pass Rate: %.2f", 
            eval.id, meanScore, worstScore, passRate));

        Assertions.assertTrue(meanScore >= eval.judge.minMeanScore, 
            "Mean score too low: " + meanScore);
        Assertions.assertTrue(worstScore >= eval.judge.minWorstScore, 
            "Worst score too low: " + worstScore);
        Assertions.assertTrue(passRate >= eval.judge.minPassRate, 
            "Pass rate too low: " + passRate);
    }


    private String generateWithRetry(String prompt, int maxRetries) throws InterruptedException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return client.generate(MODEL_NAME, prompt);
            } catch (Exception e) {

                    throw e;
}
        }
        return client.generate(MODEL_NAME, prompt); 
    }
}