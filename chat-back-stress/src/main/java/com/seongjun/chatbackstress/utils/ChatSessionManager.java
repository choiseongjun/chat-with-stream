package com.seongjun.chatbackstress.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class ChatSessionManager {
    private final Map<String, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public void addSession(String roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public List<WebSocketSession> getSessions(String roomId) {
        return roomSessions.getOrDefault(roomId, Collections.emptyList());
    }

    public List<String> getSessionIds(String roomId) {
        return getSessions(roomId).stream()
                .map(WebSocketSession::getId)
                .collect(Collectors.toList());
    }

    public void removeSession(String sessionId) {
        roomSessions.values().forEach(sessions -> 
            sessions.removeIf(session -> session.getId().equals(sessionId))
        );
    }

    public boolean isSessionInRoom(String roomId, WebSocketSession session) {
        List<WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions != null && sessions.stream()
                .anyMatch(s -> s.getId().equals(session.getId()));
    }
}
