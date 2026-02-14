package cn.lyp.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record AppConfig(
        String mode,
        String apiKey,
        String baseUrl,
        String model,
        boolean stream,
        Double temperature,
        int maxSteps,
        boolean allowShell,
        boolean mcpEnabled,
        List<McpServerConfig> mcpServers,
        boolean typewriter,
        int typewriterDelayMs,
        boolean consoleLog,
        String serverHost,
        int serverPort
) {
    public static AppConfig fromArgs(String[] args) {
        Map<String, String> cli = parseArgs(args);
        Map<String, String> fileConfig = ConfigLoader.load(resolveConfigPath(cli));

        String mode = firstNonBlank(
                cli.get("mode"),
                System.getenv("AGENT_MODE"),
                fileConfig.get("mode"),
                "client"
        );
        String apiKey = firstNonBlank(
                cli.get("api-key"),
                System.getenv("AGENT_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                fileConfig.get("api-key")
        );
        String baseUrl = firstNonBlank(
                cli.get("base-url"),
                System.getenv("AGENT_BASE_URL"),
                System.getenv("OPENAI_BASE_URL"),
                fileConfig.get("base-url"),
                "https://api.openai.com/v1"
        );
        String model = firstNonBlank(
                cli.get("model"),
                System.getenv("AGENT_MODEL"),
                fileConfig.get("model")
        );
        boolean stream = parseBoolean(firstNonBlank(
                cli.get("stream"),
                System.getenv("AGENT_STREAM"),
                fileConfig.get("stream")
        ), false);
        Double temperature = parseDouble(firstNonBlank(
                cli.get("temperature"),
                System.getenv("AGENT_TEMPERATURE"),
                fileConfig.get("temperature")
        ));
        int maxSteps = parseInt(firstNonBlank(
                cli.get("max-steps"),
                System.getenv("AGENT_MAX_STEPS"),
                fileConfig.get("max-steps")
        ), 4);
        boolean allowShell = parseBoolean(firstNonBlank(
                cli.get("allow-shell"),
                System.getenv("AGENT_ALLOW_SHELL"),
                fileConfig.get("allow-shell")
        ), false);
        boolean mcpEnabled = parseBoolean(firstNonBlank(
                cli.get("mcp-enabled"),
                System.getenv("AGENT_MCP_ENABLED"),
                fileConfig.get("mcp.enabled")
        ), false);
        String mcpServers = firstNonBlank(
                cli.get("mcp-servers"),
                System.getenv("AGENT_MCP_SERVERS"),
                fileConfig.get("mcp.servers")
        );
        List<McpServerConfig> mcpServerConfigs = parseMcpServers(mcpServers, cli, fileConfig);
        boolean typewriter = parseBoolean(firstNonBlank(
                cli.get("typewriter"),
                System.getenv("AGENT_TYPEWRITER"),
                fileConfig.get("typewriter")
        ), false);
        int typewriterDelayMs = parseInt(firstNonBlank(
                cli.get("typewriter-delay-ms"),
                System.getenv("AGENT_TYPEWRITER_DELAY_MS"),
                fileConfig.get("typewriter-delay-ms")
        ), 15);
        boolean consoleLog = parseBoolean(firstNonBlank(
                cli.get("console-log"),
                System.getenv("AGENT_CONSOLE_LOG"),
                fileConfig.get("console-log")
        ), true);
        String serverHost = firstNonBlank(
                cli.get("server-host"),
                System.getenv("AGENT_SERVER_HOST"),
                fileConfig.get("server-host")
        );
        if (serverHost == null || serverHost.isBlank()) {
            serverHost = isServerMode(mode) ? "0.0.0.0" : "127.0.0.1";
        }
        int serverPort = parseInt(firstNonBlank(
                cli.get("server-port"),
                System.getenv("AGENT_SERVER_PORT"),
                fileConfig.get("server-port")
        ), 8080);
        return new AppConfig(mode, apiKey, baseUrl, model, stream, temperature, maxSteps, allowShell,
                mcpEnabled, mcpServerConfigs, typewriter, typewriterDelayMs, consoleLog, serverHost, serverPort);
    }

    public void validate() {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Missing mode. Set --mode to server or client.");
        }
        String normalized = mode.trim().toLowerCase(java.util.Locale.ROOT);
        if (!"server".equals(normalized) && !"client".equals(normalized)) {
            throw new IllegalArgumentException("Invalid mode. Use server or client.");
        }
        if (serverHost == null || serverHost.isBlank()) {
            throw new IllegalArgumentException("Missing server host. Set --server-host or AGENT_SERVER_HOST.");
        }
        if (serverPort <= 0) {
            throw new IllegalArgumentException("Invalid server port. Set --server-port or AGENT_SERVER_PORT.");
        }
        if (!isServer()) {
            return;
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Missing model. Set --model or AGENT_MODEL.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing API key. Set --api-key or AGENT_API_KEY.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Missing base URL. Set --base-url or AGENT_BASE_URL.");
        }
    }

    public boolean isServer() {
        return isServerMode(mode);
    }

    private static boolean isServerMode(String mode) {
        return mode != null && mode.trim().equalsIgnoreCase("server");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static Double parseDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static List<McpServerConfig> parseMcpServers(
            String listValue,
            Map<String, String> cli,
            Map<String, String> fileConfig
    ) {
        if (listValue == null || listValue.isBlank()) {
            return List.of();
        }
        List<McpServerConfig> servers = new ArrayList<>();
        String[] names = listValue.split(",");
        for (String rawName : names) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim();
            String envName = normalizeEnvName(name);
            String url = firstNonBlank(
                    cli.get("mcp-server." + name + ".url"),
                    System.getenv("AGENT_MCP_SERVER_" + envName + "_URL"),
                    fileConfig.get("mcp.server." + name + ".url")
            );
            String auth = firstNonBlank(
                    cli.get("mcp-server." + name + ".auth"),
                    System.getenv("AGENT_MCP_SERVER_" + envName + "_AUTH"),
                    fileConfig.get("mcp.server." + name + ".auth")
            );
            servers.add(new McpServerConfig(name, url, auth));
        }
        return servers;
    }

    private static String normalizeEnvName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        if (args == null) {
            return out;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            String value = "true";
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                value = args[i + 1];
                i++;
            }
            out.put(key, value);
        }
        return out;
    }

    private static java.nio.file.Path resolveConfigPath(Map<String, String> cli) {
        String path = firstNonBlank(cli.get("config"), System.getenv("AGENT_CONFIG"));
        if (path != null) {
            return java.nio.file.Path.of(path);
        }
        java.nio.file.Path defaultPath = java.nio.file.Path.of("config", "agent.properties");
        if (java.nio.file.Files.exists(defaultPath)) {
            return defaultPath;
        }
        java.nio.file.Path fallbackPath = java.nio.file.Path.of("agent.properties");
        if (java.nio.file.Files.exists(fallbackPath)) {
            return fallbackPath;
        }
        return null;
    }
}
