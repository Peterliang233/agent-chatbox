package cn.lyp.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolCallParserTest {

    @Test
    void parsesToolCallWithArgs() {
        String json = "{\"type\":\"tool_call\",\"tool\":\"calc\",\"args\":{\"expression\":\"1+2\"}}";
        ToolCall call = ToolCallParser.tryParse(json);

        assertNotNull(call);
        assertTrue(call.isToolCall());
        assertEquals("calc", call.tool());
        assertEquals("1+2", call.args().get("expression"));
    }

    @Test
    void parsesFinalResponse() {
        String json = "{\"type\":\"final\",\"content\":\"done\"}";
        ToolCall call = ToolCallParser.tryParse(json);

        assertNotNull(call);
        assertTrue(call.isFinal());
        assertEquals("done", call.content());
    }

    @Test
    void returnsNullOnInvalidPayloads() {
        assertNull(ToolCallParser.tryParse(null));
        assertNull(ToolCallParser.tryParse("not json"));
        assertNull(ToolCallParser.tryParse("[]"));
        assertNull(ToolCallParser.tryParse("{\"type\":\"tool_call\"}"));
        assertNull(ToolCallParser.tryParse("{\"type\":\"tool_call\",\"tool\":\"\"}"));
    }

    @Test
    void finalWithoutContentDefaultsToEmptyString() {
        ToolCall call = ToolCallParser.tryParse("{\"type\":\"final\"}");
        assertNotNull(call);
        assertTrue(call.isFinal());
        assertEquals("", call.content());
    }
}
