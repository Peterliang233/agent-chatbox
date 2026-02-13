package cn.lyp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void parsesCliArgs() {
        String[] args = new String[]{
                "--api-key", "key",
                "--model", "model-x",
                "--stream", "true",
                "--max-steps", "7",
                "--temperature", "0.2",
                "--allow-shell", "true"
        };

        AppConfig config = AppConfig.fromArgs(args);

        assertEquals("key", config.apiKey());
        assertEquals("model-x", config.model());
        assertTrue(config.stream());
        assertEquals(7, config.maxSteps());
        assertEquals(0.2, config.temperature());
        assertTrue(config.allowShell());
        assertEquals("https://api.siliconflow.cn/v1", config.baseUrl());
    }

    @Test
    void validateRequiresKeyModelAndBaseUrl() {
        AppConfig config = new AppConfig(null, null, null, false, null, 4, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
