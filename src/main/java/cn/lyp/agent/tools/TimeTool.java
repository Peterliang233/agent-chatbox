package cn.lyp.agent.tools;

import cn.lyp.agent.Tool;
import cn.lyp.agent.ToolResult;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

public class TimeTool implements Tool {
    @Override
    public String name() {
        return "time";
    }

    @Override
    public String description() {
        return "Return the current local time.";
    }

    @Override
    public Map<String, String> args() {
        return Collections.emptyMap();
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return ToolResult.ok(now);
    }
}
