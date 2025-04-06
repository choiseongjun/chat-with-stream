import { create } from 'zustand';
import { User } from '@/types';
import { api } from '@/lib/api';

interface AuthState {
    user: User | null;
    loading: boolean;
    login: (username: string,userId:string) => Promise<void>;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set:any) => ({
    user: typeof window !== 'undefined' ? JSON.parse(localStorage.getItem('user') || 'null') : null,
    loading: false,

    login: async (username: string,userId:string) => {
        set({ loading: true });
        try {
            const user = await api.login(username,userId);
            localStorage.setItem('user', JSON.stringify(user));
            set({ user, loading: false });
        } catch (error) {
            set({ loading: false });
            throw error;
        }
    },

    logout: () => {
        localStorage.removeItem('user');
        set({ user: null });
    },
}));