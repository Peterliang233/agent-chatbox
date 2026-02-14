package cn.lyp.client;

import cn.lyp.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

public class AgentClient {
    private static final Logger logger = LoggerFactory.getLogger(AgentClient.class);
    private final HttpClient httpClient;
    private final String baseUrl;

    public AgentClient(AppConfig config) {
        this.baseUrl = "http://" + config.serverHost() + ":" + config.serverPort();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String chat(String message, boolean stream, Consumer<String> onToken) throws Exception {
        String encoded = URLEncoder.encode(Boolean.toString(stream), StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "/chat?stream=" + encoded);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("Accept", "text/event-stream")
                .build();
        logger.info("Sending chat request (SSE). stream={} messageLength={}", stream, message == null ? 0 : message.length());
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            ensureOk(response.statusCode(), errorBody);
        }
        StringBuilder full = new StringBuilder();
        try (InputStream in = response.body()) {
            parseSse(in, onToken, full);
        }
        return full.toString();
    }

    public void reset() throws Exception {
        URI uri = URI.create(baseUrl + "/reset");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureOk(response.statusCode(), response.body());
    }

    public String fetchTools() throws Exception {
        URI uri = URI.create(baseUrl + "/tools");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureOk(response.statusCode(), response.body());
        return response.body();
    }

    public String fetchConfig() throws Exception {
        URI uri = URI.create(baseUrl + "/config");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureOk(response.statusCode(), response.body());
        return response.body();
    }

    private void ensureOk(int status, String body) {
        if (status >= 200 && status < 300) {
            return;
        }
        String detail = body == null ? "" : body;
        throw new IllegalStateException("Server returned status " + status + ". " + detail);
    }

    private void parseSse(InputStream in, Consumer<String> onToken, StringBuilder full) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        String event = null;
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                dispatchEvent(event, data, onToken, full);
                if ("done".equalsIgnoreCase(event)) {
                    break;
                }
                event = null;
                data.setLength(0);
                continue;
            }
            if (line.startsWith("event:")) {
                event = line.substring(6).trim();
                continue;
            }
            if (line.startsWith("data:")) {
                String value = line.substring(5);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                if (data.length() > 0) {
                    data.append("\n");
                }
                data.append(value);
            }
        }
    }

    private void dispatchEvent(String event, StringBuilder data, Consumer<String> onToken, StringBuilder full) {
        String payload = data.toString();
        if (payload.isBlank() && (event == null || event.isBlank())) {
            return;
        }
        if ("done".equalsIgnoreCase(event) || "[DONE]".equals(payload)) {
            return;
        }
        if ("error".equalsIgnoreCase(event)) {
            throw new IllegalStateException(payload.isBlank() ? "Server error." : payload);
        }
        if (!payload.isEmpty()) {
            full.append(payload);
            if (onToken != null) {
                onToken.accept(payload);
            }
        }
    }
}
