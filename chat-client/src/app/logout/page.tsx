'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';

export default function LogoutPage() {
    const router = useRouter();
    const { logout } = useAuthStore();

    useEffect(() => {
        logout();
        router.push('/login');
    }, [logout, router]);

    return (
        <div className="min-h-screen flex items-center justify-center">
            <p>로그아웃 중...</p>
        </div>
    );
}