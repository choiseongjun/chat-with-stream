export type User = {
    id: number;
    username: string;
    status: 'ONLINE' | 'OFFLINE' | 'AWAY';
};

export type ChatRoom = {
    id: number;
    name: string;
    description?: string;
    memberCount: number;
};

export type Message = {
    id: number;
    roomId: number;
    senderId: number;
    senderName: string;
    content: string;
    createdAt: string;
};