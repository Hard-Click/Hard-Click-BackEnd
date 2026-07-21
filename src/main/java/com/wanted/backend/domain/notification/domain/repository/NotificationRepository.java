package com.wanted.backend.domain.notification.domain.repository;

import com.wanted.backend.domain.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    int countUnreadByReceiverId(Long receiverId);
    Optional<Notification> findById(Long notificationId);
    void updateRead(Long notificationId);
    List<Notification> findByReceiverIdWithCursor(Long receiverId, Long cursorId, int size);
    boolean existsNextPage(Long receiverId, Long lastId);
    List<Notification> saveAll(List<Notification> notifications);
    void deleteByRedirectUrlStartingWith(String urlPrefix);
}