# agent-chatbox

Client/server terminal AI agent built with the OpenAI Java SDK.

- **Server** runs the agent loop, tools, and model calls.
- **Client** handles terminal I/O and displays streaming output.
- **MCP** remote tools can be attached to the server over HTTP.

## Quick start

Set env vars (server side):

```bash
export AGENT_API_KEY="your_api_key"   # or OPENAI_API_KEY
export AGENT_MODEL="gpt-4o-mini"
export AGENT_BASE_URL="https://api.openai.com/v1"  # optional
```

Start the server (Terminal 1):

```bash
mvn -q -DskipTests exec:java \
  -Dexec.args="--mode server --server-port 8080"
```

Start the client (Terminal 2):

```bash
mvn -q -DskipTests exec:java \
  -Dexec.args="--mode client --server-host 127.0.0.1 --server-port 8080 --stream true --typewriter true"
```

## CLI flags

All flags are optional and override env vars (which override config file values).

```bash
mvn -q -DskipTests exec:java \
  -Dexec.args="--mode server --model gpt-4o-mini --base-url https://api.openai.com/v1 --max-steps 4"
```

Supported flags / env vars:

- `--mode` / `AGENT_MODE` (`server` or `client`, default `client`)
- `--server-host` / `AGENT_SERVER_HOST` (client connect host or server bind host)
- `--server-port` / `AGENT_SERVER_PORT`
- `--api-key` / `AGENT_API_KEY` / `OPENAI_API_KEY` (server only)
- `--base-url` / `AGENT_BASE_URL` / `OPENAI_BASE_URL` (server only; defaults to `https://api.openai.com/v1`)
- `--model` / `AGENT_MODEL` (server only)
- `--stream` / `AGENT_STREAM` (client default for chat requests)
- `--temperature` / `AGENT_TEMPERATURE`
- `--max-steps` / `AGENT_MAX_STEPS`
- `--allow-shell` / `AGENT_ALLOW_SHELL`
- `--mcp-enabled` / `AGENT_MCP_ENABLED`
- `--mcp-servers` / `AGENT_MCP_SERVERS` (comma-separated names, e.g. `files,search`)
- `--mcp-server.<name>.url` / `AGENT_MCP_SERVER_<NAME>_URL`
- `--mcp-server.<name>.auth` / `AGENT_MCP_SERVER_<NAME>_AUTH` (optional `Authorization` header)
- `--typewriter` / `AGENT_TYPEWRITER`
- `--typewriter-delay-ms` / `AGENT_TYPEWRITER_DELAY_MS` (default 15)
- `--console-log` / `AGENT_CONSOLE_LOG` (default true)
- `--config` / `AGENT_CONFIG` (default `config/agent.properties` if it exists)

## Terminal commands (client)

- `/help` show help
- `/tools` list tools (from server)
- `/config` show client + server config
- `/reset` clear server agent memory
- `/exit` quit

## Tools (server)

- `time` current local time
- `calc` basic math evaluator
- `shell` run a local command (disabled by default; enable with `--allow-shell true`)
- `mcp.<server>.<tool>` remote MCP tools discovered from configured MCP servers

## Notes

- Tool calls use a JSON protocol defined in the system prompt.
- Streaming output uses SSE (`text/event-stream`) between client and server for real-time display. To avoid streaming tool-call JSON, the server uses a second model call with a plain-text-only prompt when streaming is enabled.
- Typewriter output requires streaming (`--stream true --typewriter true`).
- MCP integration uses the official MCP Java SDK and connects to MCP servers over HTTP (SSE transport).
- Terminal input uses JLine for line editing, history, and tab completion.

## Logging

- Logs are written to `log/agent.log` with daily rolling files.
- Console logs are enabled by default.
  - Disable with `--console-log false` or `AGENT_CONSOLE_LOG=false`.

## Config file

Config file values are loaded first, then overridden by environment variables, then CLI flags.

Example `config/agent.properties`:

```properties
mode=server
server-host=0.0.0.0
server-port=8080
api-key=your_api_key
model=gpt-4o-mini
base-url=https://api.openai.com/v1
mcp.enabled=false
mcp.servers=files,search
mcp.server.files.url=http://127.0.0.1:3001
mcp.server.files.auth=Bearer your_token
mcp.server.search.url=http://127.0.0.1:3002
stream=true
typewriter=true
typewriter-delay-ms=15
console-log=true
```

## Terminal UX

- Input editing supports arrow keys, search (`Ctrl+R`), and history.
- History is persisted to `~/.agent-chatbox/history`.
- For the cleanest prompt, set `--console-log false` or `AGENT_CONSOLE_LOG=false`.

## MCP setup

1. Start one or more MCP servers that expose tools via HTTP.
2. Configure `mcp.enabled=true` and list server names in `mcp.servers`.
3. Provide each server URL (and optional `Authorization` header).

The agent will decide whether to call MCP tools based on the request. MCP tool names are prefixed as `mcp.<server>.<tool>` in `/tools` output.

## Tests

```bash
mvn -q test
```

To include the shell tool test:

```bash
RUN_SHELL_TOOL_TESTS=true mvn -q test
```
