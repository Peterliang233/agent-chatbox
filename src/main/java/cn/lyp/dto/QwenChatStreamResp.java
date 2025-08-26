package cn.lyp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QwenChatStreamResp {
    private String model;
    private String created_at;
    private String response;
    private String done;

    public QwenChatStreamResp(String model, String created_at, String response, String done) {
        this.model = model;
        this.created_at = created_at;
        this.response = response;
        this.done = done;
    }

    public QwenChatStreamResp() {
    }

    public String getModel() {
        return model;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getResponse() {
        return response;
    }

    public String getDone() {
        return done;
    }

    @Override
    public String toString() {
        return "QwenChatStreamResp{" +
                "model='" + model + '\'' +
                ", created_at='" + created_at + '\'' +
                ", response='" + response + '\'' +
                ", done='" + done + '\'' +
                '}';
    }
}
