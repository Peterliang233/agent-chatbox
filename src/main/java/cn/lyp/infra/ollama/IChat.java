package cn.lyp.infra.ollama;

import java.io.IOException;

public interface IChat {
    String Chat(String input) throws IOException, InterruptedException;
    void ChatWithJson(String input);
}
