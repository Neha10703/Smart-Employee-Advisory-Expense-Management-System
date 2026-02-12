package com.smart.app.repository;

import com.smart.app.model.UserSession;
import com.smart.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);
    
    List<UserSession> findByUserAndIsActiveTrueOrderByLastAccessedAtDesc(User user);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user = :user AND s.sessionId != :currentSessionId")
    void deactivateOtherSessions(@Param("user") User user, @Param("currentSessionId") String currentSessionId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :now WHERE s.sessionId = :sessionId")
    void updateLastAccessed(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
}