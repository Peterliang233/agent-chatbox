package cn.lyp.agent;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    Map<String, String> args();
    ToolResult execute(Map<String, Object> args) throws Exception;
}
