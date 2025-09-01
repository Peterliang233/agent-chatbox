package cn.lyp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionResponse(
        @JsonProperty("model")
        String model,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("response")
        String response,
        @JsonProperty("done")
        Boolean done,
        @JsonProperty("done_reason")
        String doneReason
) {

}
