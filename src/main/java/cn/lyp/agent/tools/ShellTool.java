package cn.lyp.agent.tools;

import cn.lyp.agent.Tool;
import cn.lyp.agent.ToolResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellTool implements Tool {
    private final Duration timeout;

    public ShellTool(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public String name() {
        return "shell";
    }

    @Override
    public String description() {
        return "Run a shell command on the local machine.";
    }

    @Override
    public Map<String, String> args() {
        return Collections.singletonMap("command", "string");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) throws Exception {
        Object command = args.get("command");
        if (command == null || command.toString().isBlank()) {
            return ToolResult.error("Missing argument: command");
        }
        List<String> cmd = buildCommand(command.toString());
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return ToolResult.error("Command timed out after " + timeout.toSeconds() + "s");
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        if (output.length() == 0) {
            output.append("(no output)");
        }
        return ToolResult.ok(output.toString().trim());
    }

    private List<String> buildCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return List.of("cmd", "/c", command);
        }
        return List.of("bash", "-lc", command);
    }
}
