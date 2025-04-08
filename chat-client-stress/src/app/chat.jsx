'use client';

import React, { useEffect, useRef, useState } from 'react';

export default function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [connected, setConnected] = useState(false);
    const [roomJoined, setRoomJoined] = useState(false);
    const [reconnectCount, setReconnectCount] = useState(0);

    // useRef로 참조하여 불필요한 리렌더링 방지
    const ws = useRef(null);
    const messagesEndRef = useRef(null);
    const socketInitialized = useRef(false);
    const reconnectTimer = useRef(null);
    const enterRoomTimer = useRef(null);

    const sender = useRef('user_' + Math.floor(Math.random() * 10000));
    const roomId = 'room1';

    // 메시지가 추가될 때마다 스크롤을 맨 아래로 이동
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    // 메시지가 변경될 때마다 스크롤을 아래로 이동
    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    // 방 입장 메시지 전송
    const enterRoom = () => {
        // WebSocket이 열려있는지 직접 확인 (상태 변수에 의존하지 않음)
        if (!ws.current || ws.current.readyState !== WebSocket.OPEN) {
            console.log(`방 입장 실패 - WebSocket 준비되지 않음. 상태: ${ws.current?.readyState}`);
            return false;
        }

        // 이미 입장한 경우 중복 입장 방지
        if (roomJoined) {
            console.log('이미 방에 입장되어 있습니다.');
            return true;
        }

        try {
            console.log('방 입장 시도...');

            const enterMessage = {
                type: 'ENTER',
                roomId,
                sender: sender.current,
                message: sender.current + '님이 입장했습니다.',
                timestamp: new Date().toISOString()
            };

            console.log('방 입장 메시지 전송:', JSON.stringify(enterMessage));
            ws.current.send(JSON.stringify(enterMessage));
            setRoomJoined(true);
            return true;
        } catch (error) {
            console.error('방 입장 메시지 전송 실패:', error);
            return false;
        }
    };

    // WebSocket 연결 설정 (컴포넌트 마운트 시 1회만 실행)
    useEffect(() => {
        // React Strict Mode 대응
        if (socketInitialized.current) {
            console.log('이미 WebSocket이 초기화되어 있습니다.');
            return;
        }

        console.log('WebSocket 초기화 시작');
        socketInitialized.current = true;

        // WebSocket 연결 함수
        const connectWebSocket = () => {
            // 이미 연결된 경우 새 연결 시도하지 않음
            if (ws.current && ws.current.readyState === WebSocket.OPEN) {
                console.log('이미 WebSocket이 연결되어 있습니다.');
                return;
            }

            // 기존 타이머 정리
            if (reconnectTimer.current) {
                clearTimeout(reconnectTimer.current);
                reconnectTimer.current = null;
            }

            if (enterRoomTimer.current) {
                clearTimeout(enterRoomTimer.current);
                enterRoomTimer.current = null;
            }

            // 기존 연결이 있으면서 닫힌 상태가 아니면 명시적으로 종료
            if (ws.current && ws.current.readyState !== WebSocket.CLOSED) {
                try {
                    console.log('이전 WebSocket 연결 명시적 종료');
                    ws.current.close();
                } catch (err) {
                    console.error('이전 WebSocket 연결 종료 중 오류:', err);
                }
            }

            // 새 WebSocket 연결 설정
            console.log('새 WebSocket 연결 시도...');
            ws.current = new WebSocket('ws://localhost:8080/ws/chat');

            ws.current.onopen = () => {
                console.log('✅ WebSocket 연결 성공');
                setConnected(true);
                setReconnectCount(0); // 재연결 카운트 초기화

                // 연결 즉시 방 입장 시도 - 타이머 사용하지 않고 바로 시도
                console.log('WebSocket 연결 후 즉시 방 입장 시도');
                enterRoom();
            };

            ws.current.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);

                    // 타입에 따라 처리
                    if (message.type === 'HISTORY') {
                        // 중복 메시지 방지를 위한 ID 기반 필터링
                        setMessages(prev => {
                            const messageId = `${message.sender}_${message.timestamp}`;
                            const isDuplicate = prev.some(m =>
                                m.sender === message.sender &&
                                m.timestamp === message.timestamp &&
                                m.message === message.message
                            );

                            if (isDuplicate) return prev;
                            return [...prev, { ...message, id: messageId }];
                        });
                    } else if (message.type === 'CHAT') {
                        // 새 채팅 메시지 추가
                        setMessages(prev => [...prev, message]);
                    }
                } catch (error) {
                    console.error('메시지 처리 오류:', error);
                }
            };

            ws.current.onclose = (event) => {
                const isCleanClose = event.code === 1000;
                console.log(`WebSocket 연결 종료: 코드=${event.code}, 사유=${event.reason}`);

                // 상태 업데이트
                setConnected(false);
                setRoomJoined(false);

                // 비정상 종료시에만 재연결 (사용자가 명시적으로 종료한 경우 제외)
                if (!isCleanClose && socketInitialized.current) {
                    const reconnectDelay = Math.min(5000, 1000 * Math.pow(1.5, reconnectCount));
                    console.log(`${reconnectDelay}ms 후 재연결 시도 예정 (시도 ${reconnectCount + 1})`);

                    // 재연결 시도
                    setReconnectCount(prev => prev + 1);
                    reconnectTimer.current = setTimeout(() => {
                        connectWebSocket();
                    }, reconnectDelay);
                }
            };

            ws.current.onerror = (error) => {
                console.error('WebSocket 오류:', error);
                // 오류 이벤트는 일반적으로 close 이벤트가 뒤따르므로 여기서 재연결하지 않음
            };
        };

        // 초기 WebSocket 연결 시작
        connectWebSocket();

        // 컴포넌트 언마운트 시 정리
        return () => {
            console.log('컴포넌트 언마운트: 정리 작업');
            socketInitialized.current = false;

            // 타이머 정리
            if (reconnectTimer.current) {
                clearTimeout(reconnectTimer.current);
                reconnectTimer.current = null;
            }

            if (enterRoomTimer.current) {
                clearTimeout(enterRoomTimer.current);
                enterRoomTimer.current = null;
            }

            // WebSocket 연결 정리
            if (ws.current) {
                try {
                    ws.current.close(1000, '사용자가 페이지를 떠남');
                } catch (err) {
                    console.error('WebSocket 연결 종료 중 오류:', err);
                }
                ws.current = null;
            }
        };
    }, []); // 빈 의존성 배열로 마운트 시 한 번만 실행

    // 메시지 전송 함수
    const sendMessage = () => {
        if (!input.trim() || !connected || !roomJoined) return;

        try {
            const message = {
                type: 'CHAT',
                roomId,
                sender: sender.current,
                message: input,
                timestamp: new Date().toISOString()
            };

            console.log('메시지 전송:', message);
            ws.current.send(JSON.stringify(message));
            setInput('');
        } catch (error) {
            console.error('메시지 전송 실패:', error);
            alert('메시지 전송에 실패했습니다. 연결 상태를 확인해주세요.');
        }
    };

    // 타임스탬프 포맷팅 함수
    const formatTimestamp = (timestamp) => {
        if (!timestamp) return '';

        try {
            const date = new Date(timestamp);
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } catch (error) {
            return '';
        }
    };

    // 수동 방 입장 기능
    const handleManualRoomJoin = () => {
        if (connected) {
            setRoomJoined(false); // 상태 리셋
            enterRoom();
        } else {
            alert('서버에 연결되어 있지 않습니다. 페이지를 새로고침해 주세요.');
        }
    };

    // 메시지 정렬 함수 (타임스탬프 기준)
    const sortedMessages = [...messages].sort((a, b) => {
        if (!a.timestamp || !b.timestamp) return 0;
        return new Date(a.timestamp) - new Date(b.timestamp);
    });

    return (
        <div style={{ padding: 20 }}>
            <h2>💬 채팅방: {roomId}</h2>
            {connected && roomJoined ? (
                <div style={{ color: 'green', marginBottom: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>✅ 채팅방에 연결되었습니다</span>
                    <button
                        onClick={handleManualRoomJoin}
                        style={{
                            padding: '4px 8px',
                            fontSize: '12px',
                            backgroundColor: '#4caf50',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                        }}
                    >
                        채팅 기록 다시 가져오기
                    </button>
                </div>
            ) : connected ? (
                <div style={{ color: 'orange', marginBottom: 10 }}>
                    ⏳ 채팅방에 입장 중...
                </div>
            ) : (
                <div style={{ color: 'red', marginBottom: 10 }}>
                    ❌ 서버에 연결 중... {reconnectCount > 0 ? `(재연결 시도 ${reconnectCount})` : ''}
                </div>
            )}

            <div style={{
                border: '1px solid #ccc',
                padding: 10,
                height: 300,
                overflowY: 'scroll',
                marginBottom: 10,
                display: 'flex',
                flexDirection: 'column',
                backgroundColor: '#ffffff'
            }}>
                <div style={{ flexGrow: 1 }}>
                    {sortedMessages.length === 0 && (
                        <div style={{ textAlign: 'center', color: '#888', padding: '20px 0' }}>
                            아직 메시지가 없습니다
                        </div>
                    )}

                    {sortedMessages.map((msg, idx) => (
                        <div key={msg.id || idx} style={{
                            padding: '8px 12px',
                            marginBottom: '8px',
                            borderRadius: '8px',
                            maxWidth: '80%',
                            backgroundColor: '#ffffff',
                            color: '#000000',
                            border: '1px solid #e0e0e0',
                            alignSelf: msg.sender === sender.current ? 'flex-end' : 'flex-start',
                            boxShadow: '0 1px 1px rgba(0,0,0,0.05)',
                            marginLeft: msg.sender === sender.current ? 'auto' : '0',
                            marginRight: msg.sender === sender.current ? '0' : 'auto',
                            position: 'relative'
                        }}>
                            <div style={{ fontWeight: 'bold', marginBottom: '4px', color: '#000000' }}>
                                {msg.sender === sender.current ? '나' : msg.sender}
                            </div>
                            <div style={{ color: '#000000' }}>{msg.message}</div>
                            <div style={{
                                fontSize: '10px',
                                color: '#555',
                                textAlign: 'right',
                                marginTop: '4px'
                            }}>
                                {formatTimestamp(msg.timestamp)}
                            </div>
                        </div>
                    ))}
                    {/* 이 요소는 스크롤 대상으로 사용됩니다 */}
                    <div ref={messagesEndRef} />
                </div>
            </div>

            <div style={{ display: 'flex' }}>
                <input
                    type="text"
                    value={input}
                    placeholder="메시지 입력"
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
                    style={{
                        padding: 10,
                        width: '80%',
                        borderRadius: '4px',
                        border: '1px solid #ccc'
                    }}
                    disabled={!connected || !roomJoined}
                />
                <button
                    onClick={sendMessage}
                    style={{
                        padding: 10,
                        marginLeft: 10,
                        minWidth: '60px',
                        backgroundColor: connected && roomJoined ? '#4caf50' : '#ccc',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: connected && roomJoined ? 'pointer' : 'default'
                    }}
                    disabled={!connected || !roomJoined}
                >
                    전송
                </button>
            </div>

            {!connected && (
                <div style={{ color: 'red', marginTop: '10px' }}>
                    연결이 끊어졌습니다. 자동으로 재연결을 시도합니다...
                </div>
            )}

            <div style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
                <div>🔧 상태 정보:</div>
                <div>- WebSocket 상태: {connected ? '연결됨' : '연결 안됨'}</div>
                <div>- 채팅방 입장 상태: {roomJoined ? '입장 완료' : '미입장'}</div>
                <div>- 메시지 수: {messages.length}</div>
                <div>- 사용자 ID: {sender.current}</div>
            </div>
        </div>
    );
}