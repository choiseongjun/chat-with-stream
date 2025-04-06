'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import { subscribeToRoomMessages } from '@/lib/api';
import MessageList from '@/components/message-list';
import MessageInput from '@/components/message-input';

export default function ChatRoomPage() {
    const params = useParams<{ roomId: string }>();
    const roomId = parseInt(params.roomId as string);
    const router = useRouter();

    const { user } = useAuthStore();
    const { currentRoom, messages, loading, selectRoom, addMessage } = useChatStore();

    useEffect(() => {
        if (!user) {
            router.push('/login');
            return;
        }

        let unsubscribe: () => void;

        const fetchRoomData = async () => {
            try {
                await selectRoom(roomId);

                // 메시지 스트림 구독
                unsubscribe = subscribeToRoomMessages(roomId, (message) => {
                    addMessage(message);
                });
            } catch (error) {
                console.error('채팅방 정보를 불러오는데 실패했습니다', error);
            }
        };

        fetchRoomData();

        return () => {
            unsubscribe?.();
        };
    }, [user, roomId, router, selectRoom, addMessage]);

    if (!user || !currentRoom) {
        if (loading) {
            return <div className="min-h-screen flex items-center justify-center">로딩 중...</div>;
        }
        return null;
    }

    return (
        <div className="min-h-screen flex flex-col">
            <header className="bg-white border-b p-4 flex justify-between items-center">
                <div className="flex items-center gap-2">
                    <Link href="/chat" className="text-blue-500">
                        ← 채팅방 목록
                    </Link>
                    <span className="font-bold mx-2">|</span>
                    <h1 className="text-xl font-bold">{currentRoom.name}</h1>
                </div>
                <div className="text-sm text-gray-500">
                    {currentRoom.memberCount}명 참여 중
                </div>
            </header>

            <div className="flex-1 flex flex-col overflow-hidden">
                <MessageList messages={messages} />
                <MessageInput />
            </div>
        </div>
    );
}
