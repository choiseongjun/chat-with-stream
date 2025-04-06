'use client';

import { useEffect, useRef, useMemo } from 'react';
import { Message } from '@/types';
import { useAuthStore } from '@/store/authStore';

interface MessageListProps {
    messages: Message[];
}

export default function MessageList({ messages }: MessageListProps) {
    const { user } = useAuthStore();
    const endRef = useRef<HTMLDivElement>(null);

    // 메시지를 생성 시간 순으로 정렬
    const sortedMessages = useMemo(() => {
        return [...messages].sort((a, b) => a.createdAt - b.createdAt);
    }, [messages]);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [sortedMessages]);

    return (
        <div className="flex-1 overflow-y-auto p-4">
            <div className="space-y-4">
                {sortedMessages.map((message) => {
                    const isCurrentUser = Number(user?.id) === message.senderId;
                    return (
                        <div
                            key={message.id}
                            className={`flex ${isCurrentUser ? 'justify-end' : 'justify-start'}`}
                        >
                            <div
                                className={`max-w-[70%] px-4 py-2 rounded-lg ${
                                    isCurrentUser
                                        ? 'bg-blue-500 text-white rounded-br-none'
                                        : 'bg-gray-200 text-gray-800 rounded-bl-none'
                                }`}
                            >
                                {!isCurrentUser && (
                                    <div className="text-xs font-medium mb-1">{message.senderName}</div>
                                )}
                                <div>{message.content}</div>
                                <div className="text-xs opacity-70 text-right mt-1">
                                    {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </div>
                            </div>
                        </div>
                    );
                })}
                <div ref={endRef} />
            </div>
        </div>
    );
}