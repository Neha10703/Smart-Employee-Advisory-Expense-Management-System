package com.smart.app.controller;

import com.smart.app.model.Notification;
import com.smart.app.model.User;
import com.smart.app.repository.NotificationRepository;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications() {
        User currentUser = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        User currentUser = getCurrentUser();
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Map<String, String> request) {
        User currentUser = getCurrentUser();
        
        Notification notification = new Notification();
        notification.setTitle(request.get("title"));
        notification.setMessage(request.get("message"));
        
        // Convert string to enum
        String typeStr = request.get("type");
        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = Notification.NotificationType.EXPENSE_COMPLETED; 
        }
        notification.setType(type);
        
        notification.setUser(currentUser);
        notification.setIsRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        return ResponseEntity.ok("Notification marked as read");
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<String> markAllAsRead() {
        User currentUser = getCurrentUser();
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId());
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }

        return ResponseEntity.ok("All notifications marked as read");
    }
}