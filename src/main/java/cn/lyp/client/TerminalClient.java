package cn.lyp.client;

import cn.lyp.config.AppConfig;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class TerminalClient {
    private static final Logger logger = LoggerFactory.getLogger(TerminalClient.class);
    private static final String PROMPT = "you> ";
    private static final String AGENT_PROMPT = "agent> ";
    private final AppConfig config;
    private final AgentClient client;

    public TerminalClient(AppConfig config) {
        this.config = config;
        this.client = new AgentClient(config);
    }

    public void run() throws Exception {
        logger.info("Terminal client started. serverHost={}, serverPort={}, stream={}, typewriter={}, consoleLog={}",
                config.serverHost(), config.serverPort(), config.stream(), config.typewriter(), config.consoleLog());
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            PrintWriter out = terminal.writer();
            LineReader reader = buildReader(terminal);
            printBanner(out);
            while (true) {
                String line;
                try {
                    line = reader.readLine(PROMPT);
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    out.println();
                    out.println("bye");
                    out.flush();
                    logger.info("Terminal session ended (EOF).");
                    break;
                }
                if (line == null) {
                    out.println();
                    out.println("bye");
                    out.flush();
                    logger.info("Terminal session ended (EOF).");
                    break;
                }
                String input = line.trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.startsWith("/")) {
                    if (!handleCommand(input, out)) {
                        break;
                    }
                    continue;
                }

                logger.info("User input received (length={}).", input.length());
                out.print(AGENT_PROMPT);
                out.flush();
                Consumer<String> onToken = null;
                if (config.stream()) {
                    onToken = config.typewriter()
                            ? token -> TypewriterPrinter.print(out, token, config.typewriterDelayMs())
                            : token -> {
                                out.print(token);
                                out.flush();
                            };
                } else if (config.typewriter()) {
                    logger.warn("Typewriter is enabled but stream=false. Enable streaming to get typewriter effect.");
                }
                String response = client.chat(input, config.stream(), onToken);
                if (!config.stream()) {
                    out.println(response);
                }
                out.println();
                out.flush();
                logger.info("Agent response received (length={}).", response == null ? 0 : response.length());
            }
        }
    }

    private LineReader buildReader(Terminal terminal) {
        DefaultParser parser = new DefaultParser();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .history(new DefaultHistory())
                .completer(new StringsCompleter("/help", "/tools", "/config", "/reset", "/exit", "/quit"))
                .build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.setOpt(LineReader.Option.HISTORY_IGNORE_SPACE);
        Path historyPath = resolveHistoryPath();
        if (historyPath != null) {
            reader.setVariable(LineReader.HISTORY_FILE, historyPath);
        }
        return reader;
    }

    private Path resolveHistoryPath() {
        try {
            String home = System.getProperty("user.home");
            if (home == null || home.isBlank()) {
                return null;
            }
            Path dir = Path.of(home, ".agent-chatbox");
            Files.createDirectories(dir);
            return dir.resolve("history");
        } catch (Exception e) {
            logger.warn("Failed to initialize history file.", e);
            return null;
        }
    }

    private boolean handleCommand(String input, PrintWriter out) throws Exception {
        logger.info("Command received: {}", input);
        switch (input) {
            case "/exit":
            case "/quit":
                out.println("bye");
                out.flush();
                logger.info("Terminal session ended (command).");
                return false;
            case "/reset":
                client.reset();
                out.println("agent state reset");
                out.flush();
                logger.info("Agent state reset.");
                return true;
            case "/config":
                printConfig(out);
                return true;
            case "/tools":
                printTools(out);
                return true;
            case "/help":
                printHelp(out);
                return true;
            default:
                out.println("unknown command. try /help");
                out.flush();
                return true;
        }
    }

    private void printBanner(PrintWriter out) throws Exception {
        out.println("Agent Chatbox (terminal client)");
        out.println("type /help for commands. ctrl+d to exit.");
        if (config.consoleLog()) {
            out.println("tip: disable console logs for a cleaner prompt (--console-log false).");
        }
        out.flush();
        printConfig(out);
        printTools(out);
    }

    private void printConfig(PrintWriter out) throws Exception {
        out.println("client config:");
        out.println("- server: " + config.serverHost() + ":" + config.serverPort());
        out.println("- stream: " + config.stream());
        out.println("- typewriter: " + config.typewriter());
        out.println("- typewriterDelayMs: " + config.typewriterDelayMs());
        out.println("- consoleLog: " + config.consoleLog());
        String serverConfig = client.fetchConfig();
        if (serverConfig != null && !serverConfig.isBlank()) {
            out.println("server config:");
            out.println(serverConfig.trim());
        }
        out.flush();
    }

    private void printTools(PrintWriter out) throws Exception {
        String tools = client.fetchTools();
        if (tools == null || tools.isBlank()) {
            out.println("tools: none");
            out.flush();
            return;
        }
        out.println("tools:");
        out.println(tools.trim());
        out.flush();
    }

    private void printHelp(PrintWriter out) {
        out.println("commands:");
        out.println("- /help    show help");
        out.println("- /tools   list tools");
        out.println("- /config  show config");
        out.println("- /reset   clear agent memory");
        out.println("- /exit    quit");
        out.flush();
    }
}
