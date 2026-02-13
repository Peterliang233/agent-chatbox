package cn.lyp.llm;

import java.util.List;
import java.util.function.Consumer;

public interface ChatClient {
    String chat(List<ChatMessage> messages, ChatOptions options, Consumer<String> onToken) throws Exception;
}
