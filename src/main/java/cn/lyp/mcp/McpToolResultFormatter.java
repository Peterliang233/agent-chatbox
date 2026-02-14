package cn.lyp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public final class McpToolResultFormatter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpToolResultFormatter() {
    }

    public static String toText(McpSchema.CallToolResult result) {
        if (result == null) {
            return "";
        }
        Object structured = result.structuredContent();
        if (structured != null) {
            return toJson(structured);
        }
        List<?> content = result.content();
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Object item : content) {
            appendContent(out, item);
        }
        return out.toString().trim();
    }

    private static String toJson(Object structured) {
        try {
            return MAPPER.writeValueAsString(structured);
        } catch (Exception e) {
            return structured.toString();
        }
    }

    private static void appendContent(StringBuilder out, Object item) {
        if (item == null) {
            return;
        }
        if (item instanceof McpSchema.TextContent text) {
            if (text.text() != null) {
                out.append(text.text());
            }
            out.append("\n");
            return;
        }
        if (item instanceof McpSchema.ImageContent image) {
            String summary = "image(" + safe(image.mimeType()) + ",bytes=" + safeLength(image.data()) + ")";
            out.append(summary).append("\n");
            return;
        }
        if (item instanceof McpSchema.AudioContent audio) {
            String summary = "audio(" + safe(audio.mimeType()) + ",bytes=" + safeLength(audio.data()) + ")";
            out.append(summary).append("\n");
            return;
        }
        if (item instanceof McpSchema.EmbeddedResource embedded) {
            appendEmbedded(out, embedded);
            return;
        }
        if (item instanceof McpSchema.ResourceLink link) {
            out.append("resource(").append(safe(link.uri())).append(")").append("\n");
            return;
        }
        out.append(item).append("\n");
    }

    private static void appendEmbedded(StringBuilder out, McpSchema.EmbeddedResource embedded) {
        McpSchema.ResourceContents resource = embedded.resource();
        if (resource instanceof McpSchema.TextResourceContents text) {
            if (text.text() != null) {
                out.append(text.text());
            }
            out.append("\n");
            return;
        }
        if (resource instanceof McpSchema.BlobResourceContents blob) {
            out.append("resource(")
                    .append(safe(resource.uri()))
                    .append(",bytes=")
                    .append(safeLength(blob.blob()))
                    .append(")")
                    .append("\n");
            return;
        }
        if (resource != null) {
            out.append(resource).append("\n");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
