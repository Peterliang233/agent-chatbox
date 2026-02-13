package cn.lyp;

import cn.lyp.cli.TerminalApp;
import cn.lyp.config.AppConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        config.validate();
        new TerminalApp(config).run();
    }
}
