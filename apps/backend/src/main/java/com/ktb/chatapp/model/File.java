package com.ktb.chatapp.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {
    private String key;        // 서버에 저장된 파일명 (UUID 포함)
    private String originalName;    // 사용자가 업로드한 원본 파일명
    private String mimetype;        // 파일 타입
    private long size;              // 파일 크기

}