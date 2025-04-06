'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';

export default function LoginForm() {
    const [userId,setUserId] = useState("");
    const [username, setUsername] = useState('');
    const [error, setError] = useState('');
    const { login, loading } = useAuthStore();
    const router = useRouter();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!username.trim()) {
            setError('사용자 이름을 입력하세요');
            return;
        }

        try {
            window.sessionStorage.setItem("username",username)

            await login(username,userId);
            router.push('/chat');
        } catch (err) {
            setError('로그인에 실패했습니다');
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4 w-full max-w-md">
            <div>
                <label htmlFor="username" className="block text-sm font-medium mb-1">
                    사용자 이름
                </label>
                <input
                    id="userId"
                    type="text"
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                    className="w-full px-3 py-2 border rounded-md"
                    placeholder="사용자 번호를 입력하세요"
                    required
                />
                <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full px-3 py-2 border rounded-md"
                    placeholder="사용자 이름을 입력하세요"
                    required
                />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-500 text-white py-2 rounded-md hover:bg-blue-600 disabled:opacity-50"
            >
                {loading ? '로그인 중...' : '로그인'}
            </button>
        </form>
    );
}