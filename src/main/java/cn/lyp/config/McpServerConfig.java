package cn.lyp.config;

public record McpServerConfig(String name, String url, String authHeader) {
    public boolean isValid() {
        return name != null && !name.isBlank() && url != null && !url.isBlank();
    }
}
