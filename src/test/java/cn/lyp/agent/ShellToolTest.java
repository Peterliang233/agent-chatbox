package cn.lyp.agent;

import cn.lyp.agent.tools.ShellTool;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

class ShellToolTest {

    @Test
    void runsCommand() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("RUN_SHELL_TOOL_TESTS")),
                "Shell tool tests are disabled by default.");
        ShellTool tool = new ShellTool(Duration.ofSeconds(3));
        ToolResult result = tool.execute(Map.of("command", "echo hello"));

        assertTrue(result.success());
        assertTrue(result.output().toLowerCase().contains("hello"));
    }
}
