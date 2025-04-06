// @ts-nocheck

import axios from 'axios';

const API_URL =  'http://localhost:8080/api';

// Axios 인스턴스 생성
const axiosInstance = axios.create({
    baseURL: API_URL,
    // headers: {
    //     'Content-Type': 'application/json',
    // },
});

// 요청 인터셉터 추가 (인증 헤더)
axiosInstance.interceptors.request.use((config) => {
    if (typeof window !== 'undefined') {
        const user = JSON.parse(localStorage.getItem('user') || 'null');
        if (user) {
            config.headers = config.headers || {};
            config.headers['X-User-Id'] = user.id.toString();
        }
    }
    return config;
});

export async function fetchWithAuth(url: string, options: any = {}) {
    try {
        const response:any = await axiosInstance({
            url,
            ...options,
        });

        return response.data;
    } catch (error) {
        // if (axios.isAxiosError(error)) {
            throw new Error(error.response?.data?.message || 'API 요청에 실패했습니다');
        // }
        // throw error;
    }
}

export const api = {
    // 사용자 로그인 (간소화)
    login: (username: string,userId:string) => {
        // 실제로는 서버에서 인증하지만, 테스트를 위해 간단하게 처리
        return Promise.resolve({
            id: userId,
            username,
            status: 'ONLINE' as const
        });
    },

    // 채팅방 관련 API
    rooms: {
        getAll: () => fetchWithAuth('/rooms/public'),

        getById: (id: number) => fetchWithAuth(`/rooms/${id}`),

        create: (data: { maxUsers: number; name: string; description: undefined; isPrivate: boolean; userId: number }) =>
            fetchWithAuth('/rooms', {
                method: 'POST',
                data,
            }),

        join: (roomId: number) =>
            fetchWithAuth(`/rooms/${roomId}/join`, {
                method: 'POST',
            }),
    },

    // 메시지 관련 API
    messages: {
        getByRoomId: (roomId: number) =>
            fetchWithAuth(`/messages/room/${roomId}`),

        send: (data: { roomId: number; content: string }) =>
            fetchWithAuth('/messages', {
                method: 'POST',
                data,
            }),
    },
};

// SSE 메시지 스트림 구독 함수
export function subscribeToRoomMessages(roomId: number, onMessage: (message: any) => void) {
    const eventSource = new EventSource(`${API_URL}/messages/stream/room/${roomId}`);

    eventSource.onmessage = (event) => {
        try {
            const message = JSON.parse(event.data);
            onMessage(message);
        } catch (error) {
            console.error('메시지 파싱 오류:', error);
        }
    };

    eventSource.onerror = () => {
        eventSource.close();

        // 재연결 시도 (3초 후)
        setTimeout(() => {
            subscribeToRoomMessages(roomId, onMessage);
        }, 3000);
    };

    return () => eventSource.close();
}