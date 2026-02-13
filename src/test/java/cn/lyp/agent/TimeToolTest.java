package cn.lyp.agent;

import cn.lyp.agent.tools.TimeTool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeToolTest {

    @Test
    void returnsNonEmptyTimestamp() {
        TimeTool tool = new TimeTool();
        ToolResult result = tool.execute(java.util.Map.of());

        assertTrue(result.success());
        assertNotNull(result.output());
        assertFalse(result.output().isBlank());
    }
}
