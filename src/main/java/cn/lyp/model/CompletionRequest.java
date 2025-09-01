package cn.lyp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionRequest(
        /**
         * 模型名称
         */
        @JsonProperty("model")
        String model,
        /**
         * 系统提示词
         */
        @JsonProperty("prompt")
        String prompt,
        /**
         * 控制模型的一些参数
         */
        @JsonProperty("options")
        Options options,
        /**
         * 是否需要流式输出
         */
        @JsonProperty("stream")
        Boolean stream,
        /**
         * 是否需要json格式化，json
         */
        @JsonProperty("format")
        String format,

        /**
         * 提供一个base64编码的images列表
         */
        @JsonProperty("images")
        List<String> images,

        /**
         * 是否需要绕过模板系统并提供完整的提示词
         */
        @JsonProperty("raw")
        Boolean raw
) {
        public static class Builder {
                private String model;
                private String prompt;
                private Options options;
                private Boolean stream;
                private String format;
                private List<String> images;
                private Boolean raw;

                public Builder model(String model) {
                        this.model = model;
                        return this;
                }

                public Builder prompt(String prompt) {
                        this.prompt = prompt;
                        return this;
                }

                public Builder options(Options options) {
                        this.options = options;
                        return this;
                }

                public Builder stream(Boolean stream) {
                        this.stream = stream;
                        return this;
                }

                public Builder format(String format) {
                        this.format = format;
                        return this;
                }

                public Builder images(List<String> images) {
                        this.images = images;
                        return this;
                }

                public Builder raw(Boolean raw) {
                        this.raw = raw;
                        return this;
                }

                public CompletionRequest build() {
                        return new CompletionRequest(model, prompt, options, stream, format, images, raw);
                }
        }
}
