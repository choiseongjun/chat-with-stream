'use client';

import { useChatStore } from '@/store/chatStore';
import Link from 'next/link';

export default function RoomList() {
    const { rooms, currentRoom } = useChatStore();

    if (!rooms.length) {
        return (
            <div className="text-center p-4 text-gray-500">
                채팅방이 없습니다
            </div>
        );
    }

    return (
        <div className="space-y-1">
            {rooms.map((room) => (
                <Link
                    key={room.id}
                    href={`/chat/${room.id}`}
                    className={`block px-4 py-2 rounded-md hover:bg-gray-100 ${
                        currentRoom?.id === room.id ? 'bg-gray-100' : ''
                    }`}
                >
                    <div className="font-medium">{room.name}</div>
                    {room.description && (
                        <div className="text-sm text-gray-500 truncate">{room.description}</div>
                    )}
                    <div className="text-xs text-gray-400 mt-1">
                        {room.memberCount}명 참여 중
                    </div>
                </Link>
            ))}
        </div>
    );
}