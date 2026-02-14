# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/cn/lyp/client`: terminal client, HTTP client, and typewriter output.
- `src/main/java/cn/lyp/server`: agent server and HTTP handlers.
- `src/main/java/cn/lyp/agent`: agent loop, tool parsing, and tool registry.
- `src/main/java/cn/lyp/agent/tools`: built-in tools (`time`, `calc`, `shell`).
- `src/main/java/cn/lyp/llm`: OpenAI Java SDK wrapper and chat models.
- `src/main/java/cn/lyp/config`: configuration loading and log setup.
- `src/main/java/cn/lyp/mcp`: MCP client integration and tool adapters.
- `src/main/resources/logback.xml`: logging configuration.
- `src/test/java/cn/lyp`: unit tests mirroring packages.
- `config/agent.properties`: optional local configuration file.
- `log/`: runtime logs (created at startup).

## Build, Test, and Development Commands
- `mvn -q test`: run unit tests.
- Run server:
  ```bash
  mvn -q -DskipTests exec:java \
    -Dexec.args="--mode server --api-key $AGENT_API_KEY --model gpt-4o-mini"
  ```
- Run client:
  ```bash
  mvn -q -DskipTests exec:java \
    -Dexec.args="--mode client --server-host 127.0.0.1 --server-port 8080 --stream true --typewriter true"
  ```

## Coding Style & Naming Conventions
- Java 4-space indentation, no tabs.
- Classes `UpperCamelCase`, methods/fields `lowerCamelCase`.
- New tools belong in `cn/lyp/agent/tools`.

## Testing Guidelines
- Framework: JUnit 5 (`org.junit.jupiter`).
- Tests live under `src/test/java` and mirror package names.
- Shell tool tests are opt-in via `RUN_SHELL_TOOL_TESTS=true mvn -q test`.

## Commit & Pull Request Guidelines
- Prefer short conventional messages like `feat: add endpoint`.
- PRs should include: summary of behavior changes, tests run, and config/log changes.

## Configuration & Logging
- Precedence: config file < environment variables < CLI flags.
- Default config path: `config/agent.properties` (or `--config <path>`).
- Logs write to `log/agent.log`; console logging toggled by `--console-log false`.
- Streaming responses may trigger an extra model call to avoid tool-call JSON output.
- MCP servers are configured via `mcp.enabled`, `mcp.servers`, and `mcp.server.<name>.*` keys.
