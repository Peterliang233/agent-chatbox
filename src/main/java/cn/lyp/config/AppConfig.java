package cn.lyp.config;

import java.util.HashMap;
import java.util.Map;

public record AppConfig(
        String apiKey,
        String baseUrl,
        String model,
        boolean stream,
        Double temperature,
        int maxSteps,
        boolean allowShell
) {
    public static AppConfig fromArgs(String[] args) {
        Map<String, String> cli = parseArgs(args);
        String apiKey = firstNonBlank(cli.get("api-key"), System.getenv("AGENT_API_KEY"), System.getenv("OPENAI_API_KEY"));
        String baseUrl = firstNonBlank(cli.get("base-url"), System.getenv("AGENT_BASE_URL"), System.getenv("OPENAI_BASE_URL"), "https://api.openai.com/v1");
        String model = firstNonBlank(cli.get("model"), System.getenv("AGENT_MODEL"));
        boolean stream = parseBoolean(firstNonBlank(cli.get("stream"), System.getenv("AGENT_STREAM")), false);
        Double temperature = parseDouble(firstNonBlank(cli.get("temperature"), System.getenv("AGENT_TEMPERATURE")));
        int maxSteps = parseInt(firstNonBlank(cli.get("max-steps"), System.getenv("AGENT_MAX_STEPS")), 4);
        boolean allowShell = parseBoolean(firstNonBlank(cli.get("allow-shell"), System.getenv("AGENT_ALLOW_SHELL")), false);
        return new AppConfig(apiKey, baseUrl, model, stream, temperature, maxSteps, allowShell);
    }

    public void validate() {
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
}
