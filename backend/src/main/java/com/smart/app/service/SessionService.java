package com.smart.app.service;

import com.smart.app.model.User;
import com.smart.app.model.UserSession;
import com.smart.app.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private static final int SESSION_TIMEOUT_HOURS = 24;

    @Transactional
    public String createSession(User user, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(SESSION_TIMEOUT_HOURS))
                .isActive(true)
                .build();
        
        sessionRepository.save(session);
        return sessionId;
    }

    @Transactional
    public boolean validateSession(String sessionId) {
        var session = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (session.isPresent() && session.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            // Update last accessed time
            sessionRepository.updateLastAccessed(sessionId, LocalDateTime.now());
            return true;
        }
        return false;
    }

    @Transactional
    public void invalidateSession(String sessionId) {
        sessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    sessionRepository.save(session);
                });
    }

    @Transactional
    public void invalidateAllUserSessions(User user) {
        List<UserSession> sessions = sessionRepository.findByUserAndIsActiveTrueOrderByLastAccessedAtDesc(user);
        sessions.forEach(session -> session.setIsActive(false));
        sessionRepository.saveAll(sessions);
    }

    @Transactional
    public void invalidateOtherSessions(User user, String currentSessionId) {
        sessionRepository.deactivateOtherSessions(user, currentSessionId);
    }

    public List<UserSession> getUserActiveSessions(User user) {
        return sessionRepository.findByUserAndIsActiveTrueOrderByLastAccessedAtDesc(user);
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deactivateExpiredSessions(LocalDateTime.now());
    }
}