# AI Response Validation Framework
The AI response validation framwork implements an automated test AI generated chatbot responses. Since LLM reponses can vary in wording, formatting, and phrasing even for the same prompt, traditional assertion based tests like exact string matching are unrelaible. To solve this, the utility checks the reponses using combination of semantic evaluation and rule checks.

## Overview
1. Eval based test execution
   Each evaluations case includes:
  - id
  - Prompt
  - Judge block conatining statistical threshhold rule based requirements such as pass rate and mean score
  - Validation block containing keyword based requirements
 
 2. LLM interaction layer (LLMClient)
    The LLMClient is responsible only for communication with the LLM provider, in our case, Ollama. LLM can be accessed using OpenAI compatible endpoint. It can send chat completion requests, extraxt JSON response, and improve reliability against failures.

3. Semantic Scoring
   Semantic scoring is done using LLM as a judge, in addition to rule based validation, we use a judge model for scoring reponse quality from 0.0 to 1.0. The judge prompt requests JSON output ensuring we get the correct response beyond just containing the keywords.

4. Execution and testing strategies
   Each eval is executed multiple times and semantic, safety and keyword validations are done. After all run, using results following outputs are generated:
   - Pass Rate= Passed runs / Total runs
   - Mean Semantic score = Average judge score scross runs
The test only fails when the thresholds are below certain limit.

## Ways to run

### Option 1: Fully Automated Execution using CI/CD pipeline
Daily AI runs are automatically executed using Github CI/CD automated execution and output can be viewed at SDET_Swiggy repo < Actions tab < select Daily AI Evaluation < Test < Run AI Evals < go to end of the file and the AI prompt execution will be shown.

### Option 2: Using CI/CD pipeline with run button
- Go to Actions tab in the SDET_Swiggy repo.
- Select Daily AI Evaluation from the left bar.
- Click Run workflow.
- Open latest workflow < test < Run AI Evals < go to end of the file and the AI prompt execution will be shown.

### Option 3: Using Manual Execution
- Pull code from Github.
- Download and open Ollama 3.2.
- Run command mvn clean test in the terminal from project location in IDE such as VS code.
- Output of AI prompts will be visible on the console along with threshholds.

## Sample Output
<img width="1260" height="732" alt="image" src="https://github.com/user-attachments/assets/0b004e46-71e7-4034-9184-9487aa7fc7dc" />

 ## Limitations
 ### Higher execution time 
 Running multiple tests per run ensure increases the overall runtime.
 ### Parsing 
 The judge output extraction depends on valid JSON in response, and if it is not valid then the judge model may return malformed text.
 ### Policy changes over time
 LLM behaviour changes across versions so, threshhold and evals may need periodic updating to meet new business rules.
 ### Coverage of egde cases
 Edge cases and rare phrases may fail, if they are not covered by LLM validation / keywords.
 ### Resource Consumption
 LLM judging is effective for calculating the accuracy for the LLM response, but it needs high amount of resources and memory for accurate results.

