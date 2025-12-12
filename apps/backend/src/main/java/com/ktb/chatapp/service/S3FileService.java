package com.ktb.chatapp.service;

import com.ktb.chatapp.dto.ChatUploadDto;
import com.ktb.chatapp.dto.ProfileUploadDto;
import com.ktb.chatapp.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3FileService {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        // S3Presigner를 한 번만 생성 (성능 최적화)
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        log.info("S3Presigner 초기화 완료");
    }

    @PreDestroy
    public void cleanup() {
        if (presigner != null) {
            presigner.close();
            log.info("S3Presigner 종료");
        }
    }

    /**
     * 채팅 파일 업로드용 Presigned URL 생성
     */
    public ChatUploadDto chatFileUpload(String fileKey, String fileName, long size, String mimetype) {
        File file = File.builder()
                .key(fileKey)
                .originalName(fileName)
                .size(size)
                .mimetype(mimetype)
                .build();

        return ChatUploadDto.builder()
                .file(file)
                .build();
    }

    /**
     * 프로필 이미지 업로드용 Presigned URL 생성
     */
    public ProfileUploadDto profileImageUpload(String fileName) {
        String key = generateKey(fileName);
        String presignedUrl = generatePresignedPutUrl(key, "image/jpeg");

        return ProfileUploadDto.builder()
                .key(key)
                .presignedUrl(presignedUrl)
                .build();
    }

    /**
     * 파일 다운로드용 Presigned GET URL 생성
     */
    public String generatePresignedGetUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                r -> r.signatureDuration(Duration.ofHours(1)) // 다운로드는 1시간
                        .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
    }

    /**
     * 공개 URL 반환 (Presigned URL 없이)
     * 버킷이 public일 때만 사용
     */
    public String getPublicUrl(String key) {
        return key;
    }

    // ===== Private 헬퍼 메서드 =====

    /**
     * UUID가 포함된 안전한 키 생성
     */
    private String generateKey(String fileName) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + fileName;
    }

    /**
     * PUT용 Presigned URL 생성 (공통 로직)
     */
    private String generatePresignedPutUrl(String key, String mimetype) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimetype)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(putObjectRequest));

        return presignedRequest.url().toString();
    }
}