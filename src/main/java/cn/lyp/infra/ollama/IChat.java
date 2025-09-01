package cn.lyp.infra.ollama;

import java.io.IOException;

public interface IChat {
    String Chat(String input, String format, Boolean stream) throws IOException, InterruptedException;
}
