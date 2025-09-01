package cn.lyp.infra.ollama;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class QwenChatTest {

    @Test
    public void testChatReturnsNull() throws IOException, InterruptedException {
        QwenChat qwenChat = new QwenChat();
        String response = qwenChat.Chat("介绍一下阿里巴巴", "", true);
        // assert判断response是否为空
        assert response != null;
        System.out.println(response);
    }
}