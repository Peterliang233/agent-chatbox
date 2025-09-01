package cn.lyp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Options(
        @JsonProperty("temperature")
        Double temperature,
        @JsonProperty("mirostat")
        Integer mirostat,
        @JsonProperty("stop")
        String stop,
        @JsonProperty("top_p")
        Double topP,
        @JsonProperty("top_k")
        Double topK
) {
        public static class Builder {
            private Double temperature;
            private Integer mirostat;
            private String stop;
            private Double topP;
            private Double topK;

                public Builder temperature(Double temperature) {
                        this.temperature = temperature;
                        return this;
                }

                public Builder mirostat(Integer mirostat) {
                        this.mirostat = mirostat;
                        return this;
                }

                public Builder stop(String stop) {
                        this.stop = stop;
                        return this;
                }

                public Builder topP(Double topP) {
                        this.topP = topP;
                        return this;
                }

                public Builder topK(Double topK) {
                        this.topK = topK;
                        return this;
                }

                public Options build() {
                        return new Options(temperature, mirostat, stop, topP, topK);
                }
        }
}
