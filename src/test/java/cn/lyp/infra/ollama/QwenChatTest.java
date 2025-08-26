package cn.lyp.infra.ollama;

import cn.lyp.dto.QwenChatStreamResp;
import cn.lyp.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class QwenChatTest {

    @Test
    public void testChatReturnsNull() throws IOException, InterruptedException {
        QwenChat qwenChat = new QwenChat();
        String response = qwenChat.Chat("介绍一下阿里巴巴");
        QwenChatStreamResp resp = StringUtil.fromJson(response, QwenChatStreamResp.class);
        System.out.println(resp.getResponse());
    }
}