package cn.lyp.agent;

import cn.lyp.llm.ChatClient;
import cn.lyp.llm.ChatMessage;
import cn.lyp.llm.ChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Agent {
    private final ChatClient chatClient;
    private ChatOptions baseOptions;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;
    private final String systemPrompt;
    private final List<ChatMessage> history = new ArrayList<>();

    public Agent(ChatClient chatClient, ChatOptions baseOptions, ToolRegistry toolRegistry, int maxSteps, String systemPrompt) {
        this.chatClient = chatClient;
        this.baseOptions = baseOptions;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
        this.systemPrompt = systemPrompt;
        reset();
    }

    public void setOptions(ChatOptions options) {
        this.baseOptions = options;
    }

    public ChatOptions getOptions() {
        return baseOptions;
    }

    public void reset() {
        history.clear();
        history.add(ChatMessage.system(systemPrompt));
    }

    public String handle(String input, Consumer<String> onToken) throws Exception {
        history.add(ChatMessage.user(input));
        for (int step = 0; step < maxSteps; step++) {
            String response = chatClient.chat(history, baseOptions, null);
            ToolCall call = ToolCallParser.tryParse(response);
            if (call == null) {
                history.add(ChatMessage.assistant(response));
                if (onToken != null) {
                    onToken.accept(response);
                }
                return response;
            }
            if (call.isFinal()) {
                history.add(ChatMessage.assistant(call.content()));
                if (onToken != null) {
                    onToken.accept(call.content());
                }
                return call.content();
            }

            history.add(ChatMessage.assistant(response));
            ToolResult result = toolRegistry.execute(call);
            String toolResult = formatToolResult(call, result);
            history.add(ChatMessage.user(toolResult));
        }
        String message = "Tool steps exceeded. Please refine the request.";
        history.add(ChatMessage.assistant(message));
        return message;
    }

    private String formatToolResult(ToolCall call, ToolResult result) {
        StringBuilder out = new StringBuilder();
        out.append("TOOL_RESULT name=").append(call.tool()).append("\n");
        out.append(result.success() ? "OK" : "ERROR").append("\n");
        out.append(result.output());
        return out.toString();
    }
}
