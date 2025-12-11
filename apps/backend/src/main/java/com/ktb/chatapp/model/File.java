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

    // 임베디드로 사용되므로 id 불필요 (Message의 id로 충분)
    // private String id; ← 삭제

    private String key;        // 서버에 저장된 파일명 (UUID 포함)
    private String originalName;    // 사용자가 업로드한 원본 파일명
    private String mimetype;        // 파일 타입
    private long size;              // 파일 크기
    //
    //    // path는 filename으로 충분 (fileStorageLocation + filename으로 구성 가능)
    // private String path; ← 삭제

    // user는 Message의 senderId와 동일하므로 불필요
    // private String user; ← 삭제

    // uploadDate는 Message의 timestamp와 동일
    // private LocalDateTime uploadDate; ← 삭제

    /**
     * 미리보기 지원 여부 확인
     */
    public boolean isPreviewable() {
        List<String> previewableTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "video/mp4", "video/webm",
                "audio/mpeg", "audio/wav",
                "application/pdf"
        );
        return previewableTypes.contains(this.mimetype);
    }
}