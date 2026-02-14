package cn.lyp.mcp;

import cn.lyp.config.McpServerConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class McpClientManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(McpClientManager.class);
    private final Map<String, McpSyncClient> clients = new HashMap<>();
    private final Map<String, List<McpSchema.Tool>> toolCatalog = new HashMap<>();
    private final List<McpServerConfig> servers;
    private final Duration requestTimeout;

    public McpClientManager(List<McpServerConfig> servers, Duration requestTimeout) {
        this.servers = servers == null ? List.of() : List.copyOf(servers);
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
    }

    public void initialize() {
        if (servers.isEmpty()) {
            logger.info("No MCP servers configured.");
            return;
        }
        for (McpServerConfig server : servers) {
            if (server == null || server.name() == null || server.name().isBlank()) {
                continue;
            }
            String name = server.name().trim();
            if (server.url() == null || server.url().isBlank()) {
                logger.warn("MCP server {} missing url. Skipping.", name);
                continue;
            }
            if (clients.containsKey(name)) {
                continue;
            }
            try {
                HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(server.url());
                if (server.authHeader() != null && !server.authHeader().isBlank()) {
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .header("Authorization", server.authHeader());
                    builder.requestBuilder(requestBuilder);
                }
                McpSyncClient client = McpClient.sync(builder.build())
                        .requestTimeout(requestTimeout)
                        .build();
                client.initialize();
                clients.put(name, client);
                McpSchema.ListToolsResult tools = client.listTools();
                List<McpSchema.Tool> list = tools == null ? List.of() : tools.tools();
                toolCatalog.put(name, list == null ? List.of() : list);
                logger.info("MCP server ready: {} tools={}", name, toolCatalog.get(name).size());
            } catch (Exception e) {
                logger.warn("Failed to initialize MCP server: {}", name, e);
            }
        }
    }

    public Map<String, List<McpSchema.Tool>> toolCatalog() {
        if (toolCatalog.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(toolCatalog);
    }

    public McpSchema.CallToolResult callTool(String serverName, String toolName, Map<String, Object> args)
            throws Exception {
        McpSyncClient client = clients.get(serverName);
        if (client == null) {
            throw new IllegalStateException("MCP server not connected: " + serverName);
        }
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args == null ? Map.of() : args);
        return client.callTool(request);
    }

    @Override
    public void close() {
        for (Map.Entry<String, McpSyncClient> entry : clients.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.debug("Failed to close MCP client {}.", entry.getKey(), e);
            }
        }
        clients.clear();
        toolCatalog.clear();
    }

}
