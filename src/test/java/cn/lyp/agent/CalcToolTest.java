package cn.lyp.agent;

import cn.lyp.agent.tools.CalcTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalcToolTest {

    @Test
    void evaluatesExpressionWithPrecedence() {
        CalcTool tool = new CalcTool();
        ToolResult result = tool.execute(Map.of("expression", "1 + 2 * 3"));

        assertTrue(result.success());
        assertEquals("7.0", result.output());
    }

    @Test
    void evaluatesExpressionWithParentheses() {
        CalcTool tool = new CalcTool();
        ToolResult result = tool.execute(Map.of("expression", "(1 + 2) * 3"));

        assertTrue(result.success());
        assertEquals("9.0", result.output());
    }

    @Test
    void rejectsDivisionByZero() {
        CalcTool tool = new CalcTool();
        ToolResult result = tool.execute(Map.of("expression", "4 / 0"));

        assertFalse(result.success());
        assertTrue(result.output().toLowerCase().contains("division"));
    }

    @Test
    void rejectsEmptyExpression() {
        CalcTool tool = new CalcTool();
        ToolResult result = tool.execute(Map.of("expression", " "));

        assertFalse(result.success());
        assertTrue(result.output().toLowerCase().contains("empty"));
    }
}
