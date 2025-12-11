import { useState, useCallback } from 'react';
import { Toast } from '../components/Toast';
import fileService from '../services/fileService';

export const useMessageHandling = (socketRef, currentUser, router, handleSessionError, messages = [], loadingMessages = false, setLoadingMessages) => {
    const [message, setMessage] = useState('');
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [showMentionList, setShowMentionList] = useState(false);
    const [mentionFilter, setMentionFilter] = useState('');
    const [mentionIndex, setMentionIndex] = useState(0);
    const [filePreview, setFilePreview] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploadError, setUploadError] = useState(null);

    const handleMessageChange = useCallback((e) => {
        const newValue = e.target.value;
        setMessage(newValue);

        const cursorPosition = e.target.selectionStart;
        const textBeforeCursor = newValue.slice(0, cursorPosition);
        const atSymbolIndex = textBeforeCursor.lastIndexOf('@');

        if (atSymbolIndex !== -1) {
            const mentionText = textBeforeCursor.slice(atSymbolIndex + 1);
            if (!mentionText.includes(' ')) {
                setMentionFilter(mentionText.toLowerCase());
                setShowMentionList(true);
                setMentionIndex(0);
                return;
            }
        }

        setShowMentionList(false);
    }, []);

    const handleLoadMore = useCallback(() => {
        if (!socketRef.current?.connected) {
            return;
        }

        if (loadingMessages) {
            return;
        }

        const sortedMessages = [...messages].sort(
            (a, b) => new Date(a.timestamp) - new Date(b.timestamp)
        );
        const oldestMessage = sortedMessages[0];
        const beforeTimestamp = oldestMessage?.timestamp;

        if (!beforeTimestamp) {
            return;
        }

        setLoadingMessages(true);

        socketRef.current.emit('fetchPreviousMessages', {
            roomId: router?.query?.room,
            before: beforeTimestamp,
            limit: 30
        });
    }, [socketRef, router?.query?.room, loadingMessages, messages, setLoadingMessages]);

    const handleMessageSubmit = useCallback(async (messageData) => {
        if (!socketRef.current?.connected || !currentUser) {
            Toast.error('채팅 서버와 연결이 끊어졌습니다.');
            return;
        }

        const roomId = router?.query?.room;
        if (!roomId) {
            Toast.error('채팅방 정보를 찾을 수 없습니다.');
            return;
        }

        try {
            if (messageData.type === 'file') {
                console.log('📤 File message submit:', messageData);

                // ✅ 파일이 이미 업로드되어 있으므로 바로 전송!
                const fileData = messageData.fileData;

                console.log('📤 Sending to socket:', {
                    room: roomId,
                    type: 'file',
                    content: messageData.content || '',
                    fileData: {
                        key: fileData.key,
                        originalName: fileData.originalName,
                        mimetype: fileData.mimetype,
                        size: fileData.size,
                        url: fileData.url
                    }
                });

                // WebSocket으로 메시지 전송
                socketRef.current.emit('chatMessage', {
                    room: roomId,
                    type: 'file',
                    content: messageData.content || '',
                    fileData: {
                        key: fileData.key,
                        originalName: fileData.originalName,
                        mimetype: fileData.mimetype,
                        size: fileData.size
                    }
                });

                setFilePreview(null);
                setMessage('');

            } else if (messageData.content?.trim()) {
                socketRef.current.emit('chatMessage', {
                    room: roomId,
                    type: 'text',
                    content: messageData.content.trim()
                });

                setMessage('');
            }

            setShowEmojiPicker(false);
            setShowMentionList(false);

        } catch (error) {
            console.error('📤 Message submit error:', error);

            if (error.message?.includes('세션') ||
                error.message?.includes('인증') ||
                error.message?.includes('토큰')) {
                if (handleSessionError) {
                    await handleSessionError();
                }
                return;
            }

            Toast.error(error.message || '메시지 전송 중 오류가 발생했습니다.');
        }
    }, [currentUser, router, handleSessionError, socketRef]);

    const handleEmojiToggle = useCallback(() => {
        setShowEmojiPicker(prev => !prev);
    }, []);

    const getFilteredParticipants = useCallback((room) => {
        if (!room?.participants) return [];

        return room.participants.filter(user =>
            user.name.toLowerCase().includes(mentionFilter) ||
            user.email.toLowerCase().includes(mentionFilter)
        );
    }, [mentionFilter]);

    const insertMention = useCallback((messageInputRef, user) => {
        if (!messageInputRef?.current) return;

        const cursorPosition = messageInputRef.current.selectionStart;
        const textBeforeCursor = message.slice(0, cursorPosition);
        const atSymbolIndex = textBeforeCursor.lastIndexOf('@');

        if (atSymbolIndex !== -1) {
            const textBeforeAt = message.slice(0, atSymbolIndex);
            const newMessage =
                textBeforeAt +
                `@${user.name} ` +
                message.slice(cursorPosition);

            setMessage(newMessage);
            setShowMentionList(false);

            setTimeout(() => {
                const newPosition = atSymbolIndex + user.name.length + 2;
                messageInputRef.current.focus();
                messageInputRef.current.setSelectionRange(newPosition, newPosition);
            }, 0);
        }
    }, [message]);

    const removeFilePreview = useCallback(() => {
        setFilePreview(null);
        setUploadError(null);
        setUploadProgress(0);
    }, []);

    return {
        message,
        showEmojiPicker,
        showMentionList,
        mentionFilter,
        mentionIndex,
        filePreview,
        uploading,
        uploadProgress,
        uploadError,
        setMessage,
        setShowEmojiPicker,
        setShowMentionList,
        setMentionFilter,
        setMentionIndex,
        setFilePreview,
        handleMessageChange,
        handleMessageSubmit,
        handleEmojiToggle,
        handleLoadMore,
        getFilteredParticipants,
        insertMention,
        removeFilePreview
    };
};

export default useMessageHandling;