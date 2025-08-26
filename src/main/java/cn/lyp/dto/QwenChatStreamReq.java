package cn.lyp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenChatStreamReq {
    @JsonProperty("model")
    private String model;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("stream")
    private boolean stream;

    // 构造函数
    public QwenChatStreamReq(String model, String prompt, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
    }
}
