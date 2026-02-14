package cn.lyp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void parsesCliArgs() {
        java.nio.file.Path configFile = createTempConfig();
        String[] args = new String[]{
                "--config", configFile.toString(),
                "--mode", "server",
                "--api-key", "key",
                "--model", "model-x",
                "--base-url", "https://cli.example/v1",
                "--stream", "true",
                "--max-steps", "7",
                "--temperature", "0.2",
                "--allow-shell", "true",
                "--mcp-enabled", "true",
                "--mcp-servers", "files,search",
                "--mcp-server.files.url", "http://files.local",
                "--mcp-server.files.auth", "Bearer filetoken",
                "--mcp-server.search.url", "http://search.local",
                "--typewriter", "true",
                "--typewriter-delay-ms", "5",
                "--console-log", "false",
                "--server-host", "0.0.0.0",
                "--server-port", "9090"
        };

        AppConfig config = AppConfig.fromArgs(args);

        assertEquals("key", config.apiKey());
        assertEquals("model-x", config.model());
        assertTrue(config.stream());
        assertEquals(7, config.maxSteps());
        assertEquals(0.2, config.temperature());
        assertTrue(config.allowShell());
        assertTrue(config.mcpEnabled());
        assertEquals(2, config.mcpServers().size());
        assertEquals("files", config.mcpServers().get(0).name());
        assertEquals("http://files.local", config.mcpServers().get(0).url());
        assertEquals("Bearer filetoken", config.mcpServers().get(0).authHeader());
        assertTrue(config.typewriter());
        assertEquals(5, config.typewriterDelayMs());
        assertFalse(config.consoleLog());
        assertEquals("server", config.mode());
        assertEquals("0.0.0.0", config.serverHost());
        assertEquals(9090, config.serverPort());
        String expectedBaseUrl = firstNonBlank(
                "https://cli.example/v1"
        );
        assertEquals(expectedBaseUrl, config.baseUrl());
    }

    @Test
    void validateRequiresKeyModelAndBaseUrl() {
        AppConfig config = new AppConfig("server", null, null, null, false, null, 4, false,
                false, java.util.List.of(), false, 15, true,
                "127.0.0.1", 8080);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static java.nio.file.Path createTempConfig() {
        try {
            java.nio.file.Path file = java.nio.file.Files.createTempFile("agent-config", ".properties");
            java.nio.file.Files.writeString(file, "base-url=https://config.example/v1\\n");
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
