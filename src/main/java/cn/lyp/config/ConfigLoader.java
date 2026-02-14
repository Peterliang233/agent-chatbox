package cn.lyp.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static Map<String, String> load(Path path) {
        if (path == null || !Files.exists(path)) {
            return Map.of();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (Exception e) {
            return Map.of();
        }
        Map<String, String> out = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            out.put(name, props.getProperty(name));
        }
        return out;
    }
}
