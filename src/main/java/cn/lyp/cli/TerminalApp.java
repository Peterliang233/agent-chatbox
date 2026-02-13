package cn.lyp.cli;

import cn.lyp.agent.Agent;
import cn.lyp.agent.ToolRegistry;
import cn.lyp.agent.tools.CalcTool;
import cn.lyp.agent.tools.ShellTool;
import cn.lyp.agent.tools.TimeTool;
import cn.lyp.config.AppConfig;
import cn.lyp.llm.ChatClient;
import cn.lyp.llm.ChatOptions;
import cn.lyp.llm.OpenAiSdkClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

public class TerminalApp {
    private final AppConfig config;
    private final Agent agent;
    private final ToolRegistry toolRegistry;

    public TerminalApp(AppConfig config) {
        this.config = config;
        this.toolRegistry = buildTools(config);
        ChatClient client = new OpenAiSdkClient(config.apiKey(), config.baseUrl());
        ChatOptions options = new ChatOptions(config.model(), config.stream(), config.temperature());
        String systemPrompt = buildSystemPrompt(toolRegistry);
        this.agent = new Agent(client, options, toolRegistry, config.maxSteps(), systemPrompt);
    }

    public void run() throws Exception {
        printBanner();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("you> ");
            String line = reader.readLine();
            if (line == null) {
                System.out.println("\nbye");
                break;
            }
            String input = line.trim();
            if (input.isEmpty()) {
                continue;
            }
            if (input.startsWith("/")) {
                if (!handleCommand(input)) {
                    break;
                }
                continue;
            }

            System.out.print("agent> ");
            Consumer<String> onToken = config.stream() ? System.out::print : null;
            String response = agent.handle(input, onToken);
            if (!config.stream()) {
                System.out.println(response);
            }
            System.out.println();
        }
    }

    private boolean handleCommand(String input) {
        switch (input) {
            case "/exit":
            case "/quit":
                System.out.println("bye");
                return false;
            case "/reset":
                agent.reset();
                System.out.println("agent state reset");
                return true;
            case "/config":
                printConfig();
                return true;
            case "/tools":
                printTools();
                return true;
            case "/help":
                printHelp();
                return true;
            default:
                System.out.println("unknown command. try /help");
                return true;
        }
    }

    private void printBanner() {
        System.out.println("Agent Chatbox (terminal)");
        System.out.println("type /help for commands. ctrl+d to exit.");
        printConfig();
        printTools();
    }

    private void printConfig() {
        System.out.println("config:");
        System.out.println("- model: " + config.model());
        System.out.println("- baseUrl: " + config.baseUrl());
        System.out.println("- stream: " + config.stream());
        System.out.println("- temperature: " + (config.temperature() == null ? "default" : config.temperature()));
        System.out.println("- maxSteps: " + config.maxSteps());
        System.out.println("- allowShell: " + config.allowShell());
    }

    private void printTools() {
        if (toolRegistry.isEmpty()) {
            System.out.println("tools: none");
            return;
        }
        System.out.println("tools:");
        System.out.println(toolRegistry.describe());
    }

    private void printHelp() {
        System.out.println("commands:");
        System.out.println("- /help    show help");
        System.out.println("- /tools   list tools");
        System.out.println("- /config  show config");
        System.out.println("- /reset   clear agent memory");
        System.out.println("- /exit    quit");
    }

    private ToolRegistry buildTools(AppConfig config) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new TimeTool());
        registry.register(new CalcTool());
        if (config.allowShell()) {
            registry.register(new ShellTool(Duration.ofSeconds(10)));
        }
        return registry;
    }

    private String buildSystemPrompt(ToolRegistry registry) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a terminal AI agent.\n");
        prompt.append("When you need a tool, respond ONLY with JSON in one line: ");
        prompt.append("{\"type\":\"tool_call\",\"tool\":\"<name>\",\"args\":{...}}\n");
        prompt.append("When you have the final response, respond ONLY with: ");
        prompt.append("{\"type\":\"final\",\"content\":\"...\"}\n");
        prompt.append("If you do not need a tool, still return a final response JSON.\n");
        prompt.append("You will receive tool results in the format:\n");
        prompt.append("TOOL_RESULT name=<tool>\nOK|ERROR\n<output>\n\n");
        if (registry.isEmpty()) {
            prompt.append("No tools are available.\n");
        } else {
            prompt.append("Available tools:\n");
            prompt.append(registry.describe()).append("\n");
        }
        return prompt.toString().trim();
    }
}
