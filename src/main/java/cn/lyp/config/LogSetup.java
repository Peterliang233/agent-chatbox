package cn.lyp.config;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LogSetup {
    private LogSetup() {
    }

    public static void ensureLogDir() {
        try {
            Files.createDirectories(Path.of("log"));
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    public static void configureConsoleLogging(AppConfig config) {
        String level = config.consoleLog() ? "INFO" : "OFF";
        System.setProperty("CONSOLE_LEVEL", level);
    }
}
