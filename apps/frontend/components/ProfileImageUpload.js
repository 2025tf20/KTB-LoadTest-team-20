import React, { useState, useRef, useEffect } from 'react';
import { CameraIcon, CloseOutlineIcon } from '@vapor-ui/icons';
import { Button, Text, VStack, HStack } from '@vapor-ui/core';
import { useAuth } from '@/contexts/AuthContext';
import CustomAvatar from '@/components/CustomAvatar';
import { Toast } from '@/components/Toast';
import axios from 'axios';
import axiosInstance from '@/services/axios';

const ProfileImageUpload = ({ currentImage, onImageChange }) => {
    const { user } = useAuth();
    const [previewUrl, setPreviewUrl] = useState(null);
    const [error, setError] = useState('');
    const [uploading, setUploading] = useState(false);
    const fileInputRef = useRef(null);

    // 컴포넌트 마운트 시 이미지 설정
    useEffect(() => {
        if (currentImage) {
            const imageUrl = currentImage;
            setPreviewUrl(imageUrl);
        }
    }, [currentImage]);

    // 파일 검증
    const validateFile = (file) => {
        if (!file) {
            return { success: false, message: '파일이 선택되지 않았습니다.' };
        }

        // 이미지 타입 검증
        if (!file.type.startsWith('image/')) {
            return { success: false, message: '이미지 파일만 업로드할 수 있습니다.' };
        }

        // 파일 크기 제한 (5MB)
        if (file.size > 5 * 1024 * 1024) {
            return { success: false, message: '파일 크기는 5MB를 초과할 수 없습니다.' };
        }

        return { success: true };
    };

    const handleFileSelect = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        try {
            // 파일 검증
            const validationResult = validateFile(file);
            if (!validationResult.success) {
                throw new Error(validationResult.message);
            }

            setUploading(true);
            setError('');

            // 임시 미리보기 생성
            const objectUrl = URL.createObjectURL(file);
            setPreviewUrl(objectUrl);

            // 1. 백엔드에서 Presigned URL 받기
            const uploadResponse = {
                data : {
                    success : true,
                    presigndeUrl: "objectUrl",
                    imageUrl: "objectUrl"
                }
            }


            //    await axiosInstance.post(
            //    `${process.env.NEXT_PUBLIC_API_URL}/api/users/profile-image`,
            //    null,
            //    {
            //        params: { profileImage: file.name },
            //        withCredentials: true
            //    }
            //);

            if (!uploadResponse.data.success) {
                throw new Error(uploadResponse.data.message || '업로드 URL 생성 실패');
            }

            const { presignedUrl, imageUrl } = uploadResponse.data;

            // 2. S3에 직접 업로드
            //await axios.put(presignedUrl, file, {
            //    headers: {
            //        'Content-Type': file.type
            //    }
            //});

            // 3. 로컬 스토리지의 사용자 정보 업데이트
            //const updatedUser = {
            //    ...user,
            //    profileImage: imageUrl
            //};
            //localStorage.setItem('user', JSON.stringify(updatedUser));

            // 4. S3 URL로 미리보기 업데이트
            URL.revokeObjectURL(objectUrl);
            setPreviewUrl(imageUrl);

            // 부모 컴포넌트에 변경 알림
            onImageChange(imageUrl);

            Toast.success('프로필 이미지가 변경되었습니다.');

            // 전역 이벤트 발생
            window.dispatchEvent(new Event('userProfileUpdate'));

        } catch (error) {
            console.error('Image upload error:', error);
            setError(error.message || '이미지 업로드에 실패했습니다.');

            // 에러 시 이전 이미지로 복구
            if (previewUrl && previewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(previewUrl);
            }
            const originalUrl = currentImage;
            setPreviewUrl(originalUrl);

            Toast.error(error.message || '이미지 업로드에 실패했습니다.');
        } finally {
            setUploading(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    const handleRemoveImage = async () => {
        try {
            setUploading(true);
            setError('');
/*
            // 백엔드에 프로필 이미지 제거 요청
            const response = await axiosInstance.delete(
                `${process.env.NEXT_PUBLIC_API_URL}/api/users/profile-image`,
                { withCredentials: true }
            );

            if (!response.data.success) {
                throw new Error(response.data.message || '이미지 삭제에 실패했습니다.');
            }

            // 로컬 스토리지의 사용자 정보 업데이트
            const updatedUser = {
                ...user,
                profileImage: ''
            };
            localStorage.setItem('user', JSON.stringify(updatedUser));

            // 기존 objectUrl 정리
            if (previewUrl && previewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(previewUrl);
            }
*/
            setPreviewUrl(null);
            onImageChange('');

            Toast.success('프로필 이미지가 삭제되었습니다.');

            // 전역 이벤트 발생
            window.dispatchEvent(new Event('userProfileUpdate'));

        } catch (error) {
            console.error('Image removal error:', error);
            setError(error.message || '이미지 삭제에 실패했습니다.');
            Toast.error(error.message || '이미지 삭제에 실패했습니다.');
        } finally {
            setUploading(false);
        }
    };

    // 컴포넌트 언마운트 시 cleanup
    useEffect(() => {
        return () => {
            if (previewUrl && previewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(previewUrl);
            }
        };
    }, [previewUrl]);

    return (
        <VStack gap="$300" alignItems="center">
            <CustomAvatar
                user={user}
                size="xl"
                persistent={true}
                showInitials={true}
                data-testid="profile-image-avatar"
            />

            <HStack gap="$200" justifyContent="center">
                <Button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                    data-testid="profile-image-upload-button"
                >
                    <CameraIcon />
                    {uploading ? '업로드 중...' : '이미지 변경'}
                </Button>

                {previewUrl && (
                    <Button
                        type="button"
                        variant="fill"
                        colorPalette="danger"
                        onClick={handleRemoveImage}
                        disabled={uploading}
                        data-testid="profile-image-delete-button"
                    >
                        <CloseOutlineIcon />
                        이미지 삭제
                    </Button>
                )}
            </HStack>

            <input
                ref={fileInputRef}
                type="file"
                className="hidden"
                accept="image/*"
                onChange={handleFileSelect}
                data-testid="profile-image-file-input"
            />

            {error && (
                <div className="p-3 bg-red-900/20 border border-red-500 rounded-lg">
                    <Text color="$danger-100">{error}</Text>
                </div>
            )}

            {uploading && (
                <Text typography="body3" color="$hint-100">
                    이미지 업로드 중...
                </Text>
            )}
        </VStack>
    );
};

export default ProfileImageUpload;