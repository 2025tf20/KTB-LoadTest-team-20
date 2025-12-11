package com.ktb.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.chatapp.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private String fileUrl;
    private String fileName;
    private String mimetype;
    private long size;
    private String user;
    private LocalDateTime uploadDate;

    // File 엔티티에서 FileResponse로 변환하는 정적 메서드
    public static FileResponse from(File file, String url, String user, LocalDateTime uploadDate) {
        return FileResponse.builder()
                .fileName(file.getOriginalName())
                .fileUrl(url)
                .user(user)
                .mimetype(file.getMimetype())
                .size(file.getSize())
                .uploadDate(uploadDate)
                .build();
    }
}
