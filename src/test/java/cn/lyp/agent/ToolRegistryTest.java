package cn.lyp.agent;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    @Test
    void executesRegisteredTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new Tool() {
            @Override
            public String name() {
                return "echo";
            }

            @Override
            public String description() {
                return "echo tool";
            }

            @Override
            public Map<String, String> args() {
                return Collections.singletonMap("text", "string");
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                return ToolResult.ok(String.valueOf(args.get("text")));
            }
        });

        ToolResult result = registry.execute(ToolCall.toolCall("echo", Map.of("text", "hi")));
        assertTrue(result.success());
        assertEquals("hi", result.output());
    }

    @Test
    void returnsErrorOnUnknownTool() {
        ToolRegistry registry = new ToolRegistry();
        ToolResult result = registry.execute(ToolCall.toolCall("missing", Map.of()));
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown tool"));
    }
}
