import { create } from 'zustand';
import { ChatRoom, Message } from '@/types';
import { api } from '@/lib/api';

interface ChatState {
    rooms: ChatRoom[];
    currentRoom: ChatRoom | null;
    messages: Message[];
    loading: boolean;
    fetchRooms: () => Promise<void>;
    selectRoom: (roomId: number) => Promise<void>;
    sendMessage: (content: string) => Promise<void>;
    addMessage: (message: Message) => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
    rooms: [],
    currentRoom: null,
    messages: [],
    loading: false,

    fetchRooms: async () => {
        set({ loading: true });
        try {
            const rooms = await api.rooms.getAll();
            set({ rooms, loading: false });
        } catch (error) {
            set({ loading: false });
            console.error('채팅방 목록을 불러오는데 실패했습니다', error);
        }
    },

    selectRoom: async (roomId: number) => {
        set({ loading: true, messages: [] });
        try {
            const [room, messages] = await Promise.all([
                api.rooms.getById(roomId),
                api.messages.getByRoomId(roomId)
            ]);

            set({
                currentRoom: room,
                messages: messages || [],
                loading: false
            });

            return room;
        } catch (error) {
            set({ loading: false });
            console.error('채팅방 정보를 불러오는데 실패했습니다', error);
            throw error;
        }
    },

    sendMessage: async (content: string) => {
        const { currentRoom } = get();
        if (!currentRoom) return;

        try {
            await api.messages.send({
                roomId: currentRoom.id,
                content
            });
        } catch (error) {
            console.error('메시지 전송에 실패했습니다', error);
        }
    },

    addMessage: (message: Message) => {
        set((state) => ({
            messages: [...state.messages, message]
        }));
    },
}));