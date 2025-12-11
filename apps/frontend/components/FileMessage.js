import React, { useState, useEffect, useRef } from 'react';
import {
    PdfIcon as FileText,
    ImageIcon as Image,
    MovieIcon as Film,
    MusicIcon as Music,
    ErrorCircleIcon as AlertCircle
} from '@vapor-ui/icons';
import { Button, VStack, HStack } from '@vapor-ui/core';
import CustomAvatar from './CustomAvatar';
import MessageContent from './MessageContent';
import MessageActions from './MessageActions';
import FileActions from './FileActions';
import ReadStatus from './ReadStatus';
import fileService from '@/services/fileService';
import { useAuth } from '@/contexts/AuthContext';

const FileMessage = ({
                         msg = {},
                         isMine = false,
                         currentUser = null,
                         onReactionAdd,
                         onReactionRemove,
                         room = null,
                         socketRef
                     }) => {
    const { user } = useAuth();
    const [error, setError] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(() => msg?.file?.fileUrl || '');
    const messageDomRef = useRef(null);
    console.log("1")
    useEffect(() => {
        if (msg?.file?.fileUrl) {
            setError(null); // 새 파일 들어오면 에러 초기화
            setPreviewUrl(msg.file.fileUrl);
            console.debug("Preview URL (from backend):", msg.file.fileUrl);
        }
    }, [msg?.file?.fileUrl]);

    if (!msg?.file) return null;

    const formattedTime = new Date(msg.timestamp).toLocaleString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });

    const getFileIcon = () => {
        console.log("4")
        const mimetype = msg.file?.mimetype || '';
        const iconProps = { className: "w-5 h-5 flex-shrink-0" };

        if (mimetype.startsWith('image/')) return <Image {...iconProps} color="#00C853" />;
        if (mimetype.startsWith('video/')) return <Film {...iconProps} color="#2196F3" />;
        if (mimetype.startsWith('audio/')) return <Music {...iconProps} color="#9C27B0" />;
        return <FileText {...iconProps} color="#ffffff" />;
    };

    const renderAvatar = () => (
        <CustomAvatar
            user={isMine ? currentUser : msg.sender}

            size="md"
            persistent={true}
            className="shrink-0"
            showInitials={true}
        />
    );

    const handleFileDownload = async (e) => {
        console.log("5")

        e.preventDefault();
        e.stopPropagation();
        setError(null);

        try {
            console.log("6")
            if (!msg.file?.fileUrl) throw new Error("파일 URL이 없습니다.");

            await fileService.downloadFromUrl(
                msg.file.fileUrl,
                msg.file.fileName || "download"
            );

        } catch (error) {
            console.log("6")
            console.error("File download error:", error);
            setError(error.message || "파일 다운로드 중 오류가 발생했습니다.");
        }
    };

    const handleViewInNewTab = (e) => {
        console.log("7")
        e.preventDefault();
        e.stopPropagation();

        try {
            console.log("8")
            if (!msg.file?.fileUrl) throw new Error("파일 URL이 없습니다.");

            const newWindow = window.open(msg.file.fileUrl, "_blank");
            if (!newWindow) {
                throw new Error("팝업이 차단되었습니다. 팝업 차단 설정을 해제해주세요.");
            }
            newWindow.opener = null;
        } catch (error) {
            console.log("9")
            setError(error.message);
        }
    };

    const renderImagePreview = () => {
        console.log("renderImagePreview");
        console.log(previewUrl);
        return (
            <div className="bg-transparent-pattern">
                <img
                    src={previewUrl}
                    alt={msg.file.fileName}
                    className="max-w-[400px] max-h-[400px] object-cover object-center rounded-md"
                    onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = "/images/placeholder-image.png";
                        setError("이미지를 불러올 수 없습니다 우히히.");
                    }}
                    loading="lazy"
                    data-testid="file-image-preview"
                />
            </div>
        );
    }

    const renderFilePreview = () => {
        console.log("10")
        const mimetype = msg.file?.mimetype || "";
        const fileName = msg.file?.fileName || "Unknown File";
        const size = fileService.formatFileSize(msg.file?.size || 0);

        // IMAGE
        if (mimetype.startsWith("image/")) {
            console.log("testest")
            return (
                <div>
                    {renderImagePreview()}
                    <div className="flex items-center gap-2 mt-2">
                        {getFileIcon()}
                        <div className="flex-1 min-w-0">
                            <div className="text-sm font-medium truncate text-gray-200">{fileName}</div>
                            <div className="text-xs text-gray-400">{size}</div>
                        </div>
                    </div>
                    <FileActions onViewInNewTab={handleViewInNewTab} onDownload={handleFileDownload} />
                </div>
            );
        }

        // VIDEO
        if (mimetype.startsWith("video/")) {
            return (
                <div>
                    <video
                        src={previewUrl}
                        className="max-w-[400px] max-h-[400px] object-cover rounded-md"
                        controls
                    />
                    <div className="flex items-center gap-2 mt-2">
                        {getFileIcon()}
                        <div className="flex-1 min-w-0">
                            <div className="text-sm font-medium truncate text-gray-200">{fileName}</div>
                            <div className="text-xs text-gray-400">{size}</div>
                        </div>
                    </div>
                    <FileActions onViewInNewTab={handleViewInNewTab} onDownload={handleFileDownload} />
                </div>
            );
        }

        // AUDIO
        if (mimetype.startsWith("audio/")) {
            return (
                <div>
                    <audio src={previewUrl} controls className="w-full mt-3" />
                    <div className="flex items-center gap-2 mt-2">
                        {getFileIcon()}
                        <div className="flex-1">
                            <div className="text-sm font-medium truncate text-gray-200">{fileName}</div>
                            <div className="text-xs text-gray-400">{size}</div>
                        </div>
                    </div>
                    <FileActions onViewInNewTab={handleViewInNewTab} onDownload={handleFileDownload} />
                </div>
            );
        }

        // OTHER FILES (PDF, ZIP, ETC)
        return (
            <div>
                <div className="flex items-center gap-2 mt-2">
                    {getFileIcon()}
                    <div className="flex-1">
                        <div className="text-sm font-medium truncate text-gray-200">{fileName}</div>
                        <div className="text-xs text-gray-400">{size}</div>
                    </div>
                </div>
                <FileActions onViewInNewTab={handleViewInNewTab} onDownload={handleFileDownload} />
            </div>
        );
    };

    return (
        <div className="my-4" ref={messageDomRef} data-testid="file-message-container">
            <VStack
                className={`max-w-[65%] ${isMine ? "ml-auto items-end" : "mr-auto items-start"}`}
                gap="$100"
            >
                {/* Sender */}
                <HStack gap="$100" alignItems="center" className="px-1">
                    {renderAvatar()}
                    <span className="text-sm font-medium text-gray-300">
                        {isMine ? "나" : msg.sender?.name}
                    </span>
                </HStack>

                {/* Bubble */}
                <div
                    className={`relative rounded-2xl px-4 py-3 border transition-all duration-200 
                    ${isMine
                        ? "bg-gray-800 border-blue-500 hover:border-blue-400"
                        : "bg-transparent border-gray-400 hover:border-gray-300"
                    }`}
                >
                    {/* Error - 간단한 div로 대체 */}
                    {error && (
                        <div className="mb-2 p-3 bg-red-900/20 border border-red-500 rounded-lg">
                            <div className="flex items-center gap-2 text-red-400">
                                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                                <span className="text-sm">{error}</span>
                            </div>
                        </div>
                    )}

                    {/* Preview */}
                    {!error && renderFilePreview()}

                    {/* Content */}
                    {!error && msg.content && (
                        <div className="mt-3 text-base">
                            <MessageContent content={msg.content} />
                        </div>
                    )}

                    {/* Footer */}
                    <HStack
                        gap="$150"
                        justifyContent="flex-end"
                        alignItems="center"
                        className={`mt-2 pt-2 border-t ${isMine ? "border-gray-700" : "border-gray-600"}`}
                    >
                        <div className={`text-xs ${isMine ? "text-blue-400" : "text-gray-300"}`}>
                            {formattedTime}
                        </div>

                        <ReadStatus
                            messageType={msg.type}
                            participants={room?.participants || []}
                            readers={msg.readers || []}
                            messageId={msg._id}
                            messageRef={messageDomRef}
                            currentUserId={currentUser?._id || currentUser?.id}
                            socketRef={socketRef}
                        />
                    </HStack>
                </div>

                {/* Reactions, actions */}
                <MessageActions
                    messageId={msg._id}
                    messageContent={msg.content}
                    reactions={msg.reactions}
                    currentUserId={currentUser?._id || currentUser?.id}
                    onReactionAdd={onReactionAdd}
                    onReactionRemove={onReactionRemove}
                    isMine={isMine}
                    room={room}
                />
            </VStack>
        </div>
    );
};

export default React.memo(FileMessage);