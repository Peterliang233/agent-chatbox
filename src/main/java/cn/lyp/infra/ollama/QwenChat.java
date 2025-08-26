package cn.lyp.infra.ollama;

import cn.lyp.config.GlobalHttpClient;
import cn.lyp.dto.QwenChatStreamReq;
import cn.lyp.util.StringUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class QwenChat implements IChat {
    private static final String model = "qwen3:1.7b";
    private static final String host = "http://localhost:11434";

    @Autowired
    private GlobalHttpClient globalHttpClient;

    @Autowired
    private static StringUtil stringUtil;

    @Override
    public String Chat(String input) throws IOException, InterruptedException {
        String url = host + "/api/generate";
        QwenChatStreamReq req = new QwenChatStreamReq(model, input, false);
        String reqJson = StringUtil.toJson(req);
        HttpRequest request = null;
        if (reqJson != null) {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                    .build();
        }
        // 发送请求并获取响应
        HttpResponse<String> response = GlobalHttpClient.getInstance().
                send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    @Override
    public void ChatWithJson(String input) {
    }
}
