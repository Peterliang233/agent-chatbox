package cn.lyp.server;

import cn.lyp.agent.Agent;
import cn.lyp.agent.ToolRegistry;
import cn.lyp.agent.tools.CalcTool;
import cn.lyp.agent.tools.ShellTool;
import cn.lyp.agent.tools.TimeTool;
import cn.lyp.config.AppConfig;
import cn.lyp.config.McpServerConfig;
import cn.lyp.llm.ChatClient;
import cn.lyp.llm.ChatOptions;
import cn.lyp.llm.OpenAiSdkClient;
import cn.lyp.mcp.McpClientManager;
import cn.lyp.mcp.McpToolAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AgentServer {
    private static final Logger logger = LoggerFactory.getLogger(AgentServer.class);
    private final AppConfig config;
    private final Agent agent;
    private final ToolRegistry toolRegistry;
    private final McpClientManager mcpClientManager;
    private HttpServer server;
    private ExecutorService executor;

    public AgentServer(AppConfig config) {
        this.config = config;
        this.toolRegistry = buildTools(config);
        this.mcpClientManager = registerMcpTools(config, toolRegistry);
        ChatClient client = new OpenAiSdkClient(config.apiKey(), config.baseUrl());
        ChatOptions options = new ChatOptions(config.model(), config.stream(), config.temperature());
        String systemPrompt = buildSystemPrompt(toolRegistry);
        this.agent = new Agent(client, options, toolRegistry, config.maxSteps(), systemPrompt);
    }

    public void start() throws IOException {
        InetSocketAddress address = new InetSocketAddress(config.serverHost(), config.serverPort());
        this.server = HttpServer.create(address, 0);
        this.executor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        server.setExecutor(executor);

        server.createContext("/health", new HealthHandler());
        server.createContext("/chat", new ChatHandler());
        server.createContext("/reset", new ResetHandler());
        server.createContext("/tools", new ToolsHandler());
        server.createContext("/config", new ConfigHandler());

        server.start();
        logger.info("Agent server started on {}:{}", config.serverHost(), config.serverPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (mcpClientManager != null) {
            mcpClientManager.close();
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            sendStatus(exchange, 200, "ok");
        }
    }

    private class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            boolean stream = parseStream(exchange.getRequestURI());
            String input = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (input.isEmpty()) {
                sendStatus(exchange, 400, "Empty request body.");
                return;
            }
            logger.info("Chat request received. stream={} inputLength={}", stream, input.length());
            prepareSse(exchange);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                if (stream) {
                    Consumer<String> onToken = token -> writeSseEvent(out, "token", token);
                    try {
                        agent.handle(input, onToken, true);
                        writeSseEvent(out, "done", "[DONE]");
                    } catch (Exception e) {
                        logger.error("Agent stream failed.", e);
                        writeSseEvent(out, "error", e.getMessage());
                        writeSseEvent(out, "done", "[DONE]");
                    }
                    return;
                }
                try {
                    String response = agent.handle(input, null, false);
                    writeSseEvent(out, "token", response);
                    writeSseEvent(out, "done", "[DONE]");
                } catch (Exception e) {
                    logger.error("Agent request failed.", e);
                    writeSseEvent(out, "error", "Agent error: " + e.getMessage());
                    writeSseEvent(out, "done", "[DONE]");
                }
            }
        }
    }

    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            agent.reset();
            sendStatus(exchange, 200, "ok");
        }
    }

    private class ToolsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            String body = toolRegistry.isEmpty() ? "" : toolRegistry.describe();
            sendStatus(exchange, 200, body);
        }
    }

    private class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append("- model: ").append(config.model()).append("\n");
            out.append("- baseUrl: ").append(config.baseUrl()).append("\n");
            out.append("- streamDefault: ").append(config.stream()).append("\n");
            out.append("- temperature: ").append(config.temperature() == null ? "default" : config.temperature()).append("\n");
            out.append("- maxSteps: ").append(config.maxSteps()).append("\n");
            out.append("- allowShell: ").append(config.allowShell()).append("\n");
            out.append("- mcpEnabled: ").append(config.mcpEnabled()).append("\n");
            out.append("- mcpServers: ").append(formatMcpServers(config.mcpServers())).append("\n");
            sendStatus(exchange, 200, out.toString());
        }
    }

    private boolean parseStream(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return config.stream();
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "stream".equalsIgnoreCase(parts[0])) {
                return Boolean.parseBoolean(parts[1]);
            }
        }
        return config.stream();
    }

    private void prepareSse(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
    }

    private void writeSseEvent(OutputStream out, String event, String data) {
        if (data == null) {
            data = "";
        }
        try {
            if (event != null && !event.isBlank()) {
                out.write(("event: " + event + "\n").getBytes(StandardCharsets.UTF_8));
            }
            String[] lines = data.split("\\r?\\n", -1);
            for (String line : lines) {
                out.write(("data: " + line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            logger.warn("Failed to write SSE event.", e);
        }
    }

    private void sendStatus(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private ToolRegistry buildTools(AppConfig config) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new TimeTool());
        registry.register(new CalcTool());
        if (config.allowShell()) {
            registry.register(new ShellTool(Duration.ofSeconds(10)));
        }
        return registry;
    }

    private McpClientManager registerMcpTools(AppConfig config, ToolRegistry registry) {
        if (!config.mcpEnabled()) {
            return null;
        }
        McpClientManager manager = new McpClientManager(config.mcpServers(), Duration.ofSeconds(30));
        manager.initialize();
        int registered = 0;
        for (var entry : manager.toolCatalog().entrySet()) {
            String serverName = entry.getKey();
            for (var tool : entry.getValue()) {
                registry.register(new McpToolAdapter(serverName, tool, manager));
                registered++;
            }
        }
        if (registered == 0) {
            logger.warn("MCP enabled but no tools were registered.");
        } else {
            logger.info("MCP tools registered: {}", registered);
        }
        return manager;
    }

    private String buildSystemPrompt(ToolRegistry registry) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a terminal AI agent.\n");
        prompt.append("When you need a tool, respond ONLY with JSON in one line: ");
        prompt.append("{\"type\":\"tool_call\",\"tool\":\"<name>\",\"args\":{...}}\n");
        prompt.append("When you have the final response, respond ONLY with: ");
        prompt.append("{\"type\":\"final\",\"content\":\"...\"}\n");
        prompt.append("If you do not need a tool, still return a final response JSON.\n");
        prompt.append("You will receive tool results in the format:\n");
        prompt.append("TOOL_RESULT name=<tool>\nOK|ERROR\n<output>\n\n");
        prompt.append("If MCP tools are available, prefer them for external data or specialized capabilities.\n");
        prompt.append("MCP tools are prefixed like mcp.<server>.<tool>.\n");
        if (registry.isEmpty()) {
            prompt.append("No tools are available.\n");
        } else {
            prompt.append("Available tools:\n");
            prompt.append(registry.describe()).append("\n");
        }
        return prompt.toString().trim();
    }

    private String formatMcpServers(java.util.List<McpServerConfig> servers) {
        if (servers == null || servers.isEmpty()) {
            return "none";
        }
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (McpServerConfig server : servers) {
            if (server == null) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            out.append(server.name());
            if (server.url() != null && !server.url().isBlank()) {
                out.append("=").append(server.url());
            }
            first = false;
        }
        return out.toString();
    }
}
