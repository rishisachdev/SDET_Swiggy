# AI Response Validation Framework (Gemini)

## Requirements
- Java 21
- Maven
- Environment variable: `GEMINI_API_KEY`

## Run locally
```bash
export GEMINI_API_KEY="YOUR_KEY"
mvn test
```

## What this framework validates
### Deterministic checks
- Must contain keyword groups (**OR** within group, **AND** across groups)
- Must NOT contain forbidden phrases
- Minimum response length

### Semantic checks (LLM as Judge)
- Gemini 1.5 Pro evaluates Gemini 1.5 Flash response quality

### Non-determinism handling (statistical evaluation)
- Multiple runs per prompt (`runs`)
- Thresholds:
  - mean score
  - worst score
  - pass rate
