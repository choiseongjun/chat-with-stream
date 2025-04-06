'use client';

import React, { useState, FormEvent } from 'react';
import {api} from "@/lib/api";

// Mock router for demonstration
const mockRouter = {
    push: (path: string) => console.log(`Navigating to: ${path}`),
    back: () => console.log('Navigating back')
};

// Mock auth store for demonstration
const useMockAuthStore = () => ({
    user: {
        id: 1,
        name: 'Test User'
    }
});

// Mock API for demonstration
const mockApi = {
    rooms: {
        create: async (roomData: any) => {
            console.log('Creating room:', roomData);
            return { id: Date.now().toString() };
        }
    }
};

export default function NewChatRoomPage() {
    const router = mockRouter;
    const { user } = useMockAuthStore();
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        isPrivate: false,
        maxUsers: 50
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!user) {
        router.push('/login');
        return null;
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;
        setFormData({
            ...formData,
            [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
        });
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        console.log("eee==",e)

        try {
            console.log("test")
            const newRoom = await api.rooms.create({
                name: formData.name,
                description: formData.description || undefined,
                isPrivate: formData.isPrivate,
                maxUsers: formData.maxUsers,
                userId: user.id // 현재 로그인된 사용자의 ID 추가
            });

            // router.push(`/chat/${newRoom.id}`);
        } catch (err) {
            setError('채팅방을 생성하는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col">
            <header className="bg-white border-b p-4">
                <h1 className="text-xl font-bold">새 채팅방 만들기</h1>
            </header>

            <main className="flex-1 p-4 max-w-2xl mx-auto">
                {error && (
                    <div className="bg-red-50 text-red-500 p-3 rounded-md mb-4">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label htmlFor="name" className="block text-sm font-medium mb-1">
                            채팅방 이름
                        </label>
                        <input
                            id="name"
                            name="name"
                            type="text"
                            required
                            value={formData.name}
                            onChange={handleChange}
                            className="w-full px-3 py-2 border rounded-md"
                        />
                    </div>

                    <div>
                        <label htmlFor="description" className="block text-sm font-medium mb-1">
                            설명 (선택사항)
                        </label>
                        <textarea
                            id="description"
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            rows={3}
                            className="w-full px-3 py-2 border rounded-md"
                        />
                    </div>

                    <div className="flex items-center space-x-2">
                        <input
                            id="isPrivate"
                            name="isPrivate"
                            type="checkbox"
                            checked={formData.isPrivate}
                            onChange={handleChange}
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <label htmlFor="isPrivate" className="text-sm font-medium">
                            비공개 채팅방
                        </label>
                    </div>

                    <div>
                        <label htmlFor="maxUsers" className="block text-sm font-medium mb-1">
                            최대 참여 인원
                        </label>
                        <input
                            id="maxUsers"
                            name="maxUsers"
                            type="number"
                            min="2"
                            max="100"
                            value={formData.maxUsers}
                            onChange={handleChange}
                            className="w-full px-3 py-2 border rounded-md"
                        />
                    </div>

                    <div className="flex gap-4 pt-2">
                        <button
                            type="button"
                            onClick={() => router.back()}
                            className="px-4 py-2 border rounded-md"
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 bg-blue-500 text-white px-4 py-2 rounded-md disabled:opacity-50"
                        >
                            {loading ? '생성 중...' : '채팅방 생성'}
                        </button>
                    </div>
                </form>
            </main>
        </div>
    );
}