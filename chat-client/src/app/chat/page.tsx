'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import RoomList from '@/components/room-list';

export default function ChatPage() {
    const router = useRouter();
    const { user } = useAuthStore();
    const { rooms, fetchRooms, loading } = useChatStore();

    useEffect(() => {
        if (!user) {
            router.push('/login');
            return;
        }

        fetchRooms();
    }, [user, router, fetchRooms]);

    if (!user) return null;

    return (
        <div className="min-h-screen flex flex-col">
            <header className="bg-white border-b p-4 flex justify-between items-center">
                <h1 className="text-xl font-bold">채팅방 목록</h1>
                <div className="flex items-center gap-4">
                    <span>{user.username}</span>
                    <Link
                        href="/logout"
                        className="px-3 py-1.5 bg-red-500 text-white rounded-md text-sm"
                    >
                        로그아웃
                    </Link>
                </div>
            </header>

            <main className="flex-1 p-4">
                {loading ? (
                    <div className="text-center py-4">로딩 중...</div>
                ) : (
                    <>
                        <div className="mb-4 flex justify-between items-center">
                            <h2 className="text-lg font-medium">공개 채팅방</h2>
                            <Link
                                href="/chat/new"
                                className="px-3 py-1.5 bg-blue-500 text-white rounded-md text-sm"
                            >
                                새 채팅방
                            </Link>
                        </div>
                        <RoomList />
                    </>
                )}
            </main>
        </div>
    );
}
