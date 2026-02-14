package cn.lyp;

import cn.lyp.client.TerminalClient;
import cn.lyp.config.AppConfig;
import cn.lyp.config.LogSetup;
import cn.lyp.server.AgentServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) throws Exception {
        LogSetup.ensureLogDir();
        AppConfig config = AppConfig.fromArgs(args);
        LogSetup.configureConsoleLogging(config);
        config.validate();
        Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("Starting agent-chatbox in {} mode", config.mode());
        if (config.isServer()) {
            new AgentServer(config).start();
            logger.info("Server running. Press Ctrl+C to stop.");
        } else {
            new TerminalClient(config).run();
        }
    }
}
