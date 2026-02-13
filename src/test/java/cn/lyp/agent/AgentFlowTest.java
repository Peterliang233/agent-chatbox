package cn.lyp.agent;

import cn.lyp.agent.tools.CalcTool;
import cn.lyp.llm.ChatClient;
import cn.lyp.llm.ChatMessage;
import cn.lyp.llm.ChatOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class AgentFlowTest {

    @Test
    void toolCallThenFinalResponse() throws Exception {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new CalcTool());

        AtomicInteger calls = new AtomicInteger();
        ChatClient client = new ChatClient() {
            @Override
            public String chat(List<ChatMessage> messages, ChatOptions options, Consumer<String> onToken) {
                int count = calls.getAndIncrement();
                if (count == 0) {
                    return "{\"type\":\"tool_call\",\"tool\":\"calc\",\"args\":{\"expression\":\"2+3\"}}";
                }
                String last = messages.get(messages.size() - 1).content();
                assertTrue(last.contains("TOOL_RESULT"));
                assertTrue(last.contains("OK"));
                return "{\"type\":\"final\",\"content\":\"done\"}";
            }
        };

        Agent agent = new Agent(client, new ChatOptions("model", false, null), registry, 3, "system");
        String response = agent.handle("calc it", null);

        assertEquals("done", response);
        assertEquals(2, calls.get());
    }

    @Test
    void plainResponsePassesThrough() throws Exception {
        ChatClient client = (messages, options, onToken) -> "hello";
        Agent agent = new Agent(client, new ChatOptions("model", false, null), new ToolRegistry(), 2, "system");

        String response = agent.handle("hi", null);
        assertEquals("hello", response);
    }
}
