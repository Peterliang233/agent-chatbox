package cn.lyp.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    private final Map<String, Tool> tools = new HashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
        logger.info("Tool registered: {}", tool.name());
    }

    public ToolResult execute(ToolCall call) {
        if (call == null || !call.isToolCall()) {
            return ToolResult.error("Invalid tool call.");
        }
        Tool tool = tools.get(call.tool());
        if (tool == null) {
            logger.warn("Tool not found: {}", call.tool());
            return ToolResult.error("Unknown tool: " + call.tool());
        }
        try {
            return tool.execute(call.args());
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }

    public List<Tool> list() {
        return new ArrayList<>(tools.values());
    }

    public String describe() {
        StringBuilder out = new StringBuilder();
        for (Tool tool : list()) {
            out.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
            if (!tool.args().isEmpty()) {
                out.append("  args: ");
                boolean first = true;
                for (Map.Entry<String, String> entry : tool.args().entrySet()) {
                    if (!first) {
                        out.append(", ");
                    }
                    out.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
                out.append("\n");
            }
        }
        return out.toString().trim();
    }
}
