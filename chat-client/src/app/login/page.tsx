'use client';

import LoginForm from '@/components/login-form';

export default function LoginPage() {
    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="w-full max-w-md p-6 bg-white rounded-lg shadow-md">
                <h1 className="text-2xl font-bold text-center mb-6">채팅 로그인</h1>
                <LoginForm />
            </div>
        </div>
    );
}