package cn.lyp.config;

import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class GlobalHttpClient {
    private GlobalHttpClient(){}

    private static final HttpClient INSTANCE = createHttpClient();

    private static HttpClient createHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(120))
                .build();
    }

    public static HttpClient getInstance() {
        return INSTANCE;
    }
}
