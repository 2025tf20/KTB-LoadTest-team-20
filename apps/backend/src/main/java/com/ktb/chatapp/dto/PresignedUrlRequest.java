package com.ktb.chatapp.dto;

import lombok.Getter;

@Getter
public class PresignedUrlRequest {
    private String fileKey;
    private String fileName;
    private String mimeType;
    private long fileSize;
}
