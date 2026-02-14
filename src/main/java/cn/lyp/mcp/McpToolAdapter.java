package cn.lyp.mcp;

import cn.lyp.agent.Tool;
import cn.lyp.agent.ToolResult;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class McpToolAdapter implements Tool {
    private final String name;
    private final String description;
    private final Map<String, String> args;
    private final String serverName;
    private final String toolName;
    private final McpClientManager clientManager;

    public McpToolAdapter(String serverName, McpSchema.Tool tool, McpClientManager clientManager) {
        this.serverName = serverName;
        this.toolName = tool.name();
        this.clientManager = clientManager;
        this.name = buildLocalName(serverName, tool.name());
        this.description = buildDescription(serverName, tool);
        this.args = buildArgs(tool);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<String, String> args() {
        return args;
    }

    @Override
    public ToolResult execute(Map<String, Object> args) throws Exception {
        McpSchema.CallToolResult result = clientManager.callTool(serverName, toolName, args);
        String output = McpToolResultFormatter.toText(result);
        boolean isError = result != null && Boolean.TRUE.equals(result.isError());
        return isError ? ToolResult.error(output) : ToolResult.ok(output);
    }

    public static String buildLocalName(String serverName, String toolName) {
        String server = serverName == null ? "default" : serverName.trim();
        String tool = toolName == null ? "tool" : toolName.trim();
        return "mcp." + server + "." + tool;
    }

    private static String buildDescription(String serverName, McpSchema.Tool tool) {
        StringBuilder out = new StringBuilder();
        out.append("[mcp:").append(serverName).append("] ");
        if (tool.description() != null && !tool.description().isBlank()) {
            out.append(tool.description().trim());
        } else {
            out.append("Remote MCP tool.");
        }
        return out.toString();
    }

    private static Map<String, String> buildArgs(McpSchema.Tool tool) {
        Map<String, String> out = new LinkedHashMap<>();
        McpSchema.JsonSchema schema = tool.inputSchema();
        if (schema == null || schema.properties() == null || schema.properties().isEmpty()) {
            return out;
        }
        List<String> required = schema.required() == null ? List.of() : schema.required();
        for (Map.Entry<String, Object> entry : schema.properties().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = describeSchema(entry.getValue());
            if (required.contains(key)) {
                value = value.isBlank() ? "required" : value + " (required)";
            }
            out.put(key, value);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static String describeSchema(Object schema) {
        if (schema == null) {
            return "";
        }
        if (schema instanceof McpSchema.JsonSchema jsonSchema) {
            StringBuilder out = new StringBuilder();
            if (jsonSchema.type() != null && !jsonSchema.type().isBlank()) {
                out.append(jsonSchema.type());
            }
            return out.toString();
        }
        if (schema instanceof Map<?, ?> map) {
            Object type = map.get("type");
            Object desc = map.get("description");
            StringBuilder out = new StringBuilder();
            if (type != null) {
                out.append(type.toString());
            }
            if (desc != null && !desc.toString().isBlank()) {
                if (!out.isEmpty()) {
                    out.append(": ");
                }
                out.append(desc.toString().trim());
            }
            return out.toString();
        }
        return schema.toString();
    }
}
