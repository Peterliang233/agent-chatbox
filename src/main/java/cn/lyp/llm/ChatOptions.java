package cn.lyp.llm;

public record ChatOptions(
        String model,
        boolean stream,
        Double temperature
) {
    public ChatOptions withStream(boolean stream) {
        return new ChatOptions(model, stream, temperature);
    }
}
