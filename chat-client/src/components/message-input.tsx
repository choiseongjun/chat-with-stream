'use client';

import { useState, FormEvent } from 'react';
import { useChatStore } from '@/store/chatStore';

export default function MessageInput() {
    const [message, setMessage] = useState('');
    const { sendMessage } = useChatStore();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!message.trim()) return;

        try {
            await sendMessage(message);
            setMessage('');
        } catch (error) {
            console.error('메시지 전송 실패:', error);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="border-t p-4 bg-white">
            <div className="flex gap-2">
                <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="메시지를 입력하세요"
                    className="flex-1 px-3 py-2 border rounded-md"
                />
                <button
                    type="submit"
                    disabled={!message.trim()}
                    className="bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600 disabled:opacity-50"
                >
                    전송
                </button>
            </div>
        </form>
    );
}
