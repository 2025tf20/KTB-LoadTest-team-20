package com.ktb.chatapp.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ChatMessageRequest {
    private String room;
    private String type;
    private String content;
    private FileDataRequest fileData;  // ← 추가!

    @Data
    public static class FileDataRequest {
        private String key;              // S3 key
        private String originalName;
        private String mimetype;
        private Long size;
    }
    public String getNormalizedContent() {
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        return "";
    }
    public MessageContent getParsedContent() {
        return MessageContent.from(getNormalizedContent());
    }

    public String getMessageType() {
        return type != null ? type : "text";
    }

    public boolean hasFileData() {
        return fileData != null && fileData.getKey() != null;
    }

    // fileData에서 값 가져오는 헬퍼 메서드
    public String getFileName() {
        return hasFileData() ? fileData.getOriginalName() : null;
    }

    public Long getSize() {
        return hasFileData() ? fileData.getSize() : null;
    }

    public String getMimeType() {
        return hasFileData() ? fileData.getMimetype() : null;
    }

    public String getFileKey() {
        return hasFileData() ? fileData.getKey() : null;
    }
}