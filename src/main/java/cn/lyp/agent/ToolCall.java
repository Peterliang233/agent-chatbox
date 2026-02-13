package cn.lyp.agent;

import java.util.Collections;
import java.util.Map;

public record ToolCall(String type, String tool, Map<String, Object> args, String content) {
    public static ToolCall toolCall(String tool, Map<String, Object> args) {
        return new ToolCall("tool_call", tool, args == null ? Collections.emptyMap() : args, null);
    }

    public static ToolCall finalAnswer(String content) {
        return new ToolCall("final", null, Collections.emptyMap(), content == null ? "" : content);
    }

    public boolean isToolCall() {
        return "tool_call".equals(type);
    }

    public boolean isFinal() {
        return "final".equals(type);
    }
}
