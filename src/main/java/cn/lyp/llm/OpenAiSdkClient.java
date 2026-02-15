package cn.lyp.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAiSdkClient implements ChatClient {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiSdkClient.class);
    private final OpenAIClient client;

    public OpenAiSdkClient(String apiKey, String baseUrl) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                .apiKey(apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        this.client = builder.build();
    }

    @Override
    public String chat(List<ChatMessage> messages, ChatOptions options, Consumer<String> onToken) {
        logger.info("LLM request: model={}, stream={}, message={}",
                options.model(), options.stream(), messages);
        ChatCompletionCreateParams.Builder params = ChatCompletionCreateParams.builder()
                .model(options.model());
        if (options.temperature() != null) {
            params.temperature(options.temperature());
        }

        for (ChatMessage message : messages) {
            addMessage(params, message);
        }

        if (options.stream()) {
            return stream(params.build(), onToken);
        }

        ChatCompletion completion = client.chat().completions().create(params.build());
        String output = completion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .collect(Collectors.joining());
        logger.info("LLM response completed. output={}", output);
        return output;
    }

    private String stream(ChatCompletionCreateParams params, Consumer<String> onToken) {
        StringBuilder full = new StringBuilder();
        try (StreamResponse<ChatCompletionChunk> stream = client.chat().completions().createStreaming(params)) {
            stream.stream()
                    .flatMap(chunk -> chunk.choices().stream())
                    .flatMap(choice -> choice.delta().content().stream())
                    .forEach(token -> {
                        full.append(token);
                        if (onToken != null) {
                            onToken.accept(token);
                        }
                    });
        }
        logger.info("LLM stream completed. outputLength={}", full.length());
        return full.toString();
    }

    private void addMessage(ChatCompletionCreateParams.Builder params, ChatMessage message) {
        String role = message.role() == null ? "user" : message.role().toLowerCase(Locale.ROOT);
        switch (role) {
            case "system":
                params.addSystemMessage(message.content());
                return;
            case "assistant":
                params.addAssistantMessage(message.content());
                return;
            case "user":
            default:
                params.addUserMessage(message.content());
                return;
        }
    }
}
