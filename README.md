# agent-chatbox

Terminal-first AI agent built with the OpenAI Java SDK.

## Quick start

Set env vars:

```bash
export AGENT_API_KEY="your_api_key"   # or OPENAI_API_KEY
export AGENT_MODEL="gpt-4o-mini"
export AGENT_BASE_URL="https://api.openai.com/v1"  # optional
```

Run:

```bash
mvn -q -DskipTests exec:java
```

## CLI flags

All flags are optional and override env vars.

```bash
mvn -q -DskipTests exec:java \
  -Dexec.args="--model gpt-4o-mini --base-url https://api.openai.com/v1 --stream false --max-steps 4"
```

Supported flags / env vars:

- `--api-key` / `AGENT_API_KEY` / `OPENAI_API_KEY`
- `--base-url` / `AGENT_BASE_URL` / `OPENAI_BASE_URL` (defaults to `https://api.openai.com/v1`)
- `--model` / `AGENT_MODEL`
- `--stream` / `AGENT_STREAM`
- `--temperature` / `AGENT_TEMPERATURE`
- `--max-steps` / `AGENT_MAX_STEPS`
- `--allow-shell` / `AGENT_ALLOW_SHELL`

## Terminal commands

- `/help` show help
- `/tools` list tools
- `/config` show config
- `/reset` clear agent memory
- `/exit` quit

## Tools

- `time` current local time
- `calc` basic math evaluator
- `shell` run a local command (disabled by default; enable with `--allow-shell true`)

## Notes

- Tool calls use a JSON protocol defined in the system prompt.
- Streaming is buffered when tools are enabled, so output prints after tool use finishes.

## Tests

```bash
mvn -q test
```

To include the shell tool test:

```bash
RUN_SHELL_TOOL_TESTS=true mvn -q test
```
