package cn.lyp.client;

import cn.lyp.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class TerminalClient {
    private static final Logger logger = LoggerFactory.getLogger(TerminalClient.class);
    private final AppConfig config;
    private final AgentClient client;

    public TerminalClient(AppConfig config) {
        this.config = config;
        this.client = new AgentClient(config);
    }

    public void run() throws Exception {
        printBanner();
        logger.info("Terminal client started. serverHost={}, serverPort={}, stream={}, typewriter={}, consoleLog={}",
                config.serverHost(), config.serverPort(), config.stream(), config.typewriter(), config.consoleLog());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("you> ");
            String line = reader.readLine();
            if (line == null) {
                System.out.println("\nbye");
                logger.info("Terminal session ended (EOF).");
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

            logger.info("User input received (length={}).", input.length());
            System.out.print("agent> ");
            Consumer<String> onToken = null;
            if (config.stream()) {
                onToken = config.typewriter()
                        ? token -> TypewriterPrinter.print(token, config.typewriterDelayMs())
                        : System.out::print;
            } else if (config.typewriter()) {
                logger.warn("Typewriter is enabled but stream=false. Enable streaming to get typewriter effect.");
            }
            String response = client.chat(input, config.stream(), onToken);
            if (!config.stream()) {
                System.out.println(response);
            }
            if (config.stream()) {
                System.out.println();
            }
            logger.info("Agent response received (length={}).", response == null ? 0 : response.length());
        }
    }

    private boolean handleCommand(String input) throws Exception {
        logger.info("Command received: {}", input);
        switch (input) {
            case "/exit":
            case "/quit":
                System.out.println("bye");
                logger.info("Terminal session ended (command).");
                return false;
            case "/reset":
                client.reset();
                System.out.println("agent state reset");
                logger.info("Agent state reset.");
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

    private void printBanner() throws Exception {
        System.out.println("Agent Chatbox (terminal client)");
        System.out.println("type /help for commands. ctrl+d to exit.");
        printConfig();
        printTools();
    }

    private void printConfig() throws Exception {
        System.out.println("client config:");
        System.out.println("- server: " + config.serverHost() + ":" + config.serverPort());
        System.out.println("- stream: " + config.stream());
        System.out.println("- typewriter: " + config.typewriter());
        System.out.println("- typewriterDelayMs: " + config.typewriterDelayMs());
        System.out.println("- consoleLog: " + config.consoleLog());
        String serverConfig = client.fetchConfig();
        if (serverConfig != null && !serverConfig.isBlank()) {
            System.out.println("server config:");
            System.out.println(serverConfig.trim());
        }
    }

    private void printTools() throws Exception {
        String tools = client.fetchTools();
        if (tools == null || tools.isBlank()) {
            System.out.println("tools: none");
            return;
        }
        System.out.println("tools:");
        System.out.println(tools.trim());
    }

    private void printHelp() {
        System.out.println("commands:");
        System.out.println("- /help    show help");
        System.out.println("- /tools   list tools");
        System.out.println("- /config  show config");
        System.out.println("- /reset   clear agent memory");
        System.out.println("- /exit    quit");
    }
}
