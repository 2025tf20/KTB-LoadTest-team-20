package com.ktb.chatapp.service;

import com.ktb.chatapp.dto.ProfileImageResponse;
import com.ktb.chatapp.dto.ProfileUploadDto;
import com.ktb.chatapp.dto.UpdateProfileRequest;
import com.ktb.chatapp.dto.UserResponse;
import com.ktb.chatapp.model.User;
import com.ktb.chatapp.repository.UserRepository;
import com.ktb.chatapp.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final S3FileService s3FileService;

    private final MongoTemplate mongoTemplate;


    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.profile.image.max-size:5242880}") // 5MB
    private long maxProfileImageSize;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    /**
     * 현재 사용자 프로필 조회
     * @param email 사용자 이메일
     */
    @Cacheable(value = "user:profile", key = "#email")
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    /**
     * 사용자 프로필 업데이트
     * @param email 사용자 이메일
     */
    @CacheEvict(value = "user:profile", key = "#email")
    public UserResponse updateUserProfile(String email, UpdateProfileRequest request) {
        String normalizedEmail = email.toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        Query query = new Query(
            Criteria.where("email").is(normalizedEmail)
        );

        Update update = new Update()
        .set("name", request.getName())
        .set("updatedAt",now);

        FindAndModifyOptions options = FindAndModifyOptions.options()
            .returnNew(true);

        User updatedUser = mongoTemplate.findAndModify(query, update, options, User.class);

        if(updatedUser == null){
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }
        log.info("사용자 프로필 업데이트 완료 - ID: {}, Name: {}", updatedUser.getId(), updatedUser.getName());

        return UserResponse.from(updatedUser);
    }

    /**
     * 프로필 이미지 업로드
     * @param email 사용자 이메일
     */
    public ProfileImageResponse uploadProfileImage(String email, String fileName) {

        ProfileUploadDto profileUploadDto = s3FileService.profileImageUpload(fileName);

        String normalizedEmail = email.toLowerCase();

        LocalDateTime now = LocalDateTime.now();

        Query query = new Query(
            Criteria.where("email").is(normalizedEmail)
        );

        Update update = new Update()
            .set("profileImage", profileUploadDto.getKey())
            .set("updatedAt",now);

        FindAndModifyOptions options = FindAndModifyOptions.options()
            .returnNew(false);

        User previousUser = mongoTemplate.findAndModify(query, update, options, User.class);
        
        if(previousUser == null){
            try {
                //deleteOldProfileImage(profileImageUrl);
            } catch (Exception e){
                log.warn("사용자 없음으로 인해 업로드된 고아 파일 삭제 실패: {}");
            }
            
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        String oldProfileImage = previousUser.getProfileImage();
        if(oldProfileImage != null && !oldProfileImage.isEmpty()){
            //deleteOldProfileImage(oldProfileImage);
        }

        return new ProfileImageResponse(
                true,
                "프로필 이미지가 업데이트되었습니다.",
                profileUploadDto.getPresignedUrl(),
                s3FileService.getPublicUrl(profileUploadDto.getKey())
        );
    }

    /**
     * 특정 사용자 프로필 조회
     */
    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return UserResponse.from(user);
    }

    /**
     * 프로필 이미지 파일 유효성 검증
     */
    private void validateProfileImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지가 제공되지 않았습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > maxProfileImageSize) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        // 파일 확장자 검증 (보안을 위해 화이트리스트 유지)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        // FileSecurityUtil의 static 메서드 호출
        String extension = FileUtil.getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    /**
     * 기존 프로필 이미지 삭제
     */
    private void deleteOldProfileImage(String profileImageUrl) {
        try {
            if (profileImageUrl != null && profileImageUrl.startsWith("/uploads/")) {
                // URL에서 파일명 추출
                String filename = profileImageUrl.substring("/uploads/".length());
                Path filePath = Paths.get(uploadDir, filename);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("기존 프로필 이미지 삭제 완료: {}", filename);
                }
            }
        } catch (IOException e) {
            log.warn("기존 프로필 이미지 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 프로필 이미지 삭제
     * @param email 사용자 이메일
     */
    public void deleteProfileImage(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            deleteOldProfileImage(user.getProfileImage());
            user.setProfileImage("");
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("프로필 이미지 삭제 완료 - User ID: {}", user.getId());
        }
    }

    /**
     * 회원 탈퇴 처리
     * @param email 사용자 이메일
     */
    @CacheEvict(value = "user:profile", key = "#email")
    public void deleteUserAccount(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            deleteOldProfileImage(user.getProfileImage());
        }

        userRepository.delete(user);
        log.info("회원 탈퇴 완료 - User ID: {}", user.getId());
    }
}
