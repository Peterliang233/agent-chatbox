package cn.lyp.agent;

import cn.lyp.llm.ChatClient;
import cn.lyp.llm.ChatMessage;
import cn.lyp.llm.ChatOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Agent {
    private static final Logger logger = LoggerFactory.getLogger(Agent.class);
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
        return handle(input, onToken, baseOptions.stream());
    }

    public String handle(String input, Consumer<String> onToken, boolean streamResponse) throws Exception {
        history.add(ChatMessage.user(input));
        for (int step = 0; step < maxSteps; step++) {
            String response = chatClient.chat(history, baseOptions.withStream(false), null);
            logger.info("received chat response: " + response);
            ToolCall call = ToolCallParser.tryParse(response);
            if (call == null) {
                return finalizeResponse(response, onToken, streamResponse);
            }
            if (call.isFinal()) {
                return finalizeResponse(call.content(), onToken, streamResponse);
            }

            history.add(ChatMessage.assistant(response));
            logger.info("Tool call requested: {} argsKeys={}", call.tool(), call.args().keySet());
            ToolResult result = toolRegistry.execute(call);
            logger.info("Tool result: {} success={} outputLength={}", call.tool(), result.success(),
                    result.output() == null ? 0 : result.output().length());
            String toolResult = formatToolResult(call, result);
            history.add(ChatMessage.user(toolResult));
        }
        String message = "Tool steps exceeded. Please refine the request.";
        logger.warn("Max tool steps exceeded.");
        history.add(ChatMessage.assistant(message));
        return message;
    }

    private String finalizeResponse(String content, Consumer<String> onToken, boolean streamResponse) throws Exception {
        if (streamResponse && onToken != null) {
            String streamed = streamFinalResponse(onToken);
            history.add(ChatMessage.assistant(streamed));
            return streamed;
        }
        history.add(ChatMessage.assistant(content));
        if (onToken != null) {
            onToken.accept(content);
        }
        return content;
    }

    private String streamFinalResponse(Consumer<String> onToken) throws Exception {
        logger.info("Streaming final response requires an extra model call.");
        ChatOptions streamOptions = baseOptions.withStream(true);
        java.util.List<ChatMessage> replay = buildStreamingHistory();
        return chatClient.chat(replay, streamOptions, onToken);
    }

    private java.util.List<ChatMessage> buildStreamingHistory() {
        java.util.List<ChatMessage> replay = new java.util.ArrayList<>();
        replay.add(ChatMessage.system(
                "You are a helpful assistant. Respond with the final answer only. " +
                        "Do not output JSON or tool-call markers."
        ));
        for (ChatMessage message : history) {
            if (message == null) {
                continue;
            }
            String role = message.role();
            if (role == null) {
                continue;
            }
            if ("user".equalsIgnoreCase(role)) {
                replay.add(message);
                continue;
            }
            if ("assistant".equalsIgnoreCase(role)) {
                ToolCall parsed = ToolCallParser.tryParse(message.content());
                if (parsed != null) {
                    continue;
                }
                replay.add(message);
            }
        }
        replay.add(ChatMessage.user("Provide the final answer now. Plain text only."));
        return replay;
    }

    private String formatToolResult(ToolCall call, ToolResult result) {
        StringBuilder out = new StringBuilder();
        out.append("TOOL_RESULT name=").append(call.tool()).append("\n");
        out.append(result.success() ? "OK" : "ERROR").append("\n");
        out.append(result.output());
        return out.toString();
    }
}
