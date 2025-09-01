package cn.lyp.infra.ollama;

import cn.lyp.config.GlobalHttpClient;
import cn.lyp.model.CompletionRequest;
import cn.lyp.model.CompletionResponse;
import cn.lyp.model.Options;
import cn.lyp.util.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class QwenChat implements IChat {
    private static final String model = "qwen3:1.7b";
    private static final String host = "http://localhost:11434";

    @Override
    public String Chat(String input, String format, Boolean stream) throws IOException, InterruptedException {
        String url = host + "/api/generate";
        Options options = new Options.Builder().build();
        CompletionRequest req = new CompletionRequest
                .Builder()
                .model(model)
                .prompt(input)
                .stream(stream)
                .format(format)
                .options(options)
                .build();
        String reqJson = StringUtil.toJson(req);
        System.out.println(reqJson);
        HttpRequest request = null;
        if (reqJson != null) {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                    .build();
        }

        if (request == null) {
            return null;
        }
        // 发送请求并获取响应
        if (stream) {
            ObjectMapper mapper = new ObjectMapper();
            HttpResponse<InputStream> response = GlobalHttpClient.getInstance().
                    send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder fullContent = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if(line.isBlank()) continue;

                    JsonNode node = mapper.readTree(line);
                    String chunk = null;
                    // 兼容两种情况
                    if (node.has("response")) {
                        chunk = node.get("response").asText();
                    } else if (node.has("message")) {
                        chunk = node.path("message").path("content").asText();
                    }

                    if (chunk != null && !chunk.isEmpty()) {
                        fullContent.append(chunk);
                        System.out.print(chunk); // 流式拼接输出
                    }

                    if (node.path("done").asBoolean(false)) {
                        System.out.println("\n[流式结束，done_reason=" + node.path("done_reason").asText("") + "]");
                        break;
                    }
                }
                return fullContent.toString();
            }
        }else {
            HttpResponse<String> response = GlobalHttpClient.getInstance().
                    send(request, HttpResponse.BodyHandlers.ofString());
            return Objects.requireNonNull(StringUtil.fromJson(response.body(), CompletionResponse.class)).response();
        }
    }
}

