import axios, { isCancel, CancelToken } from 'axios';
import axiosInstance from './axios';
import { Toast } from '../components/Toast';

class FileService {
    constructor() {
        this.baseUrl = process.env.NEXT_PUBLIC_API_URL;
        this.s3BaseUrl = 'https://ktb-team20.s3.ap-northeast-2.amazonaws.com/uploads/';

        this.allowedTypes = {
            image: {
                extensions: ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
                mimeTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
                maxSize: 10 * 1024 * 1024,
                name: '이미지'
            },
            document: {
                extensions: ['.pdf'],
                mimeTypes: ['application/pdf'],
                maxSize: 20 * 1024 * 1024,
                name: 'PDF 문서'
            }
        };
    }

    /**
     * 파일 검증 (동기 함수로 변경)
     */
    validateFile(file) {
        if (!file) {
            const message = '파일이 선택되지 않았습니다.';
            return { success: false, message };
        }

        // MIME 타입 검증
        let isAllowedType = false;
        let maxTypeSize = 0;
        let typeConfig = null;

        for (const config of Object.values(this.allowedTypes)) {
            if (config.mimeTypes.includes(file.type)) {
                isAllowedType = true;
                maxTypeSize = config.maxSize;
                typeConfig = config;
                break;
            }
        }

        if (!isAllowedType) {
            const message = 'JPG 또는 PDF 파일만 업로드 가능합니다.';
            return { success: false, message };
        }

        // 파일 크기 검증
        if (file.size > maxTypeSize) {
            const message = `${typeConfig.name} 파일은 ${this.formatFileSize(maxTypeSize)}를 초과할 수 없습니다.`;
            return { success: false, message };
        }

        // 확장자 검증
        const ext = this.getFileExtension(file.name);
        if (!typeConfig.extensions.includes(ext.toLowerCase())) {
            const message = '파일 확장자가 올바르지 않습니다.';
            return { success: false, message };
        }

        return { success: true };
    }

    /**
     * S3 Presigned URL로 업로드
     */
    async uploadFile(file, onProgress) {
        // 검증 (동기 호출)
        const validationResult = this.validateFile(file);
        if (!validationResult.success) {
            return validationResult;
        }

        try {
            const key = `${crypto.randomUUID()}${file.name}`;
            const uploadUrl = `${this.s3BaseUrl}${key}`;
            console.log(key)
            console.log(uploadUrl)
            // 1. 백엔드에서 Presigned PUT URL 받기
            const response = await axiosInstance.post(
                `${this.baseUrl}/api/files/upload`,
                {
                    fileKey: key,
                    fileName: file.name,
                    fileSize: file.size,
                    mimeType: file.type
                },
                { withCredentials: true }
            );
            

            if (!response.data.success) {
                return {
                    success: false,
                    message: response.data.message || '업로드 URL 생성 실패'
                };
            }

            //const { presignedUrl, file: fileMetadata } = presignedResponse.data;

            // 2. S3에 직접 업로드
            await axios.put(uploadUrl, file, {
                headers: {
                    'Content-Type': file.type
                },
                onUploadProgress: (progressEvent) => {
                    if (onProgress) {
                        const percentCompleted = Math.round(
                            (progressEvent.loaded * 100) / progressEvent.total
                        );
                        onProgress(percentCompleted);
                    }
                }
            });
            // 3. 성공
            return {
                success: true,
                data: {
                    file: {
                        //...fileMetadata,
                        key: key, 
                        mimetype:file.type,
                        originalName:file.name,
                        size:file.size,
                        url: uploadUrl                        
                    }
                }
            };

        } catch (error) {
            console.error('Upload error:', error);
            return this.handleUploadError(error);
        }
    }

    /**
     * 파일 다운로드
     */
    async downloadFile(s3Key, originalname) {
        try {
            const downloadUrl = this.getS3Url(s3Key);
            const timestamp = new Date().getTime();
            const separator = downloadUrl.includes('?') ? '&' : '?';
            const urlWithCacheBuster = `${downloadUrl}${separator}t=${timestamp}`;
            const response = await axios.create().get(urlWithCacheBuster, {
                responseType: 'blob',
                timeout: 30000,
                headers: {
                    'Authorization': undefined
                }
            });
            const contentType = response.headers['content-type'];
            const finalFilename = originalname;

            const blob = new Blob([response.data], {
                type: contentType || 'application/octet-stream'
            });
            const blobUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = blobUrl;
            link.download = finalFilename;
            link.style.display = 'none';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            setTimeout(() => {
                window.URL.revokeObjectURL(blobUrl);
            }, 100);

            return { success: true };

        } catch (error) {
            if (error.response?.status === 404) {
                return { success: false, message: '파일을 찾을 수 없습니다.' };
            }
            if (error.response?.status === 403) {
                return { success: false, message: '파일에 접근할 권한이 없습니다.' };
            }

            return this.handleDownloadError(error);
        }
    }

    /**
     * S3 URL 생성
     */
    getS3Url(key) {
        return `${this.s3BaseUrl}${key}`;
    }

    /**
     * 미리보기 URL
     */
    getPreviewUrl(file) {
        console.log("!!!!!!")
        console.log(file);
        if (!file?.key) return '';
        return this.getS3Url(file.fileUrl);
    }

    getFileExtension(filename) {
        if (!filename) return '';
        const parts = filename.split('.');
        return parts.length > 1 ? `.${parts.pop().toLowerCase()}` : '';
    }

    formatFileSize(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return `${parseFloat((bytes / Math.pow(1024, i)).toFixed(2))} ${units[i]}`;
    }

    handleUploadError(error) {
        console.error('Upload error:', error);

        if (error.code === 'ECONNABORTED') {
            return {
                success: false,
                message: '파일 업로드 시간이 초과되었습니다.'
            };
        }

        if (axios.isAxiosError(error)) {
            const status = error.response?.status;
            const message = error.response?.data?.message;

            switch (status) {
                case 400:
                    return { success: false, message: message || '잘못된 요청입니다.' };
                case 401:
                    return { success: false, message: '인증이 필요합니다.' };
                case 413:
                    return { success: false, message: '파일이 너무 큽니다.' };
                case 415:
                    return { success: false, message: '지원하지 않는 파일 형식입니다.' };
                case 500:
                    return { success: false, message: '서버 오류가 발생했습니다.' };
                default:
                    return { success: false, message: message || '파일 업로드에 실패했습니다.' };
            }
        }

        return {
            success: false,
            message: error.message || '알 수 없는 오류가 발생했습니다.'
        };
    }

    handleDownloadError(error) {
        console.error('Download error:', error);

        if (error.code === 'ECONNABORTED') {
            return {
                success: false,
                message: '파일 다운로드 시간이 초과되었습니다.'
            };
        }

        if (axios.isAxiosError(error)) {
            const status = error.response?.status;
            const message = error.response?.data?.message;

            switch (status) {
                case 404:
                    return { success: false, message: '파일을 찾을 수 없습니다.' };
                case 403:
                    return { success: false, message: '파일에 접근할 권한이 없습니다.' };
                case 400:
                    return { success: false, message: message || '잘못된 요청입니다.' };
                case 500:
                    return { success: false, message: '서버 오류가 발생했습니다.' };
                default:
                    return { success: false, message: message || '파일 다운로드에 실패했습니다.' };
            }
        }

        return {
            success: false,
            message: error.message || '알 수 없는 오류가 발생했습니다.'
        };
    }
}


export default new FileService();