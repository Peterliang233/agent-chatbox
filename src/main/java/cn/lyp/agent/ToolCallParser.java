package cn.lyp.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public class ToolCallParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolCallParser() {
    }

    public static ToolCall tryParse(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null;
        }

        try {
            JsonNode node = MAPPER.readTree(trimmed);
            String type = node.path("type").asText(null);
            if (type == null || type.isBlank()) {
                return null;
            }
            if ("final".equals(type)) {
                return ToolCall.finalAnswer(node.path("content").asText(""));
            }
            if (!"tool_call".equals(type)) {
                return null;
            }
            String tool = node.path("tool").asText(null);
            if (tool == null || tool.isBlank()) {
                return null;
            }
            JsonNode argsNode = node.path("args");
            Map<String, Object> args = argsNode.isMissingNode() || argsNode.isNull()
                    ? Collections.emptyMap()
                    : MAPPER.convertValue(argsNode, new TypeReference<Map<String, Object>>() {});
            return ToolCall.toolCall(tool, args);
        } catch (Exception e) {
            return null;
        }
    }
}
