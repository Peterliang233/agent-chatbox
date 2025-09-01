package cn.lyp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record message (
        @JsonProperty("role")
        String role,
        @JsonProperty("content")
        String content
) {
}
