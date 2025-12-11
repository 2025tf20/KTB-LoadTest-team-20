package com.ktb.chatapp.model;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Message 문서 모델 정의.
 * MongoDB 필드 이름과 인덱스를 명시한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "readers_userId_idx", def = "{'readers.userId': 1}"),
        @CompoundIndex(name = "room_isDeleted_timestamp_idx", def = "{'room': 1, 'isDeleted': 1, 'timestamp': -1}")
})
public class Message {

    @Id
    private String id;

    @Indexed
    @Field("room")
    private String roomId;

    @Size(max = 10000, message = "메시지는 10000자를 초과할 수 없습니다.")
    private String content;

    @Field("sender")
    private String senderId;

    private MessageType type;

    // fileId 대신 File 객체를 직접 임베딩
    @Field("file")
    private File file;  // ← 변경: String fileId → File file

    private AiType aiType;

    @Builder.Default
    private List<String> mentions = new ArrayList<>();

    @CreatedDate
    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Set<String>> reactions = new HashMap<>();

    @Builder.Default
    private List<MessageReader> readers = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Indexed
    @Builder.Default
    private Boolean isDeleted = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageReader {
        private String userId;
        private LocalDateTime readAt;
    }

    public long toTimestampMillis() {
        return timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public boolean addReaction(String reaction, String userId) {
        if (this.reactions == null) {
            this.reactions = new HashMap<>();
        }
        Set<String> userReactions = this.reactions.computeIfAbsent(
                reaction,
                key -> new java.util.HashSet<>()
        );
        return userReactions.add(userId);
    }

    public boolean removeReaction(String reaction, String userId) {
        if (this.reactions == null) {
            return false;
        }
        Set<String> userReactions = this.reactions.get(reaction);
        if (userReactions != null && userReactions.remove(userId)) {
            if (userReactions.isEmpty()) {
                this.reactions.remove(reaction);
            }
            return true;
        }
        return false;
    }

    /**
     * 파일 메타데이터를 메시지에 첨부한다.
     * 이제 File 객체를 직접 설정
     */
    public void attachFile(File file) {
        this.file = file;
        this.type = MessageType.file; // 파일 타입으로 자동 설정
    }
}
