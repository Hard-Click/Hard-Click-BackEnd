package com.wanted.backend.domain.notification.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataNotificationRepository
        extends JpaRepository<NotificationJpaEntity, Long> {

    int countByReceiverIdAndIsRead(Long receiverId, boolean isRead);

    List<NotificationJpaEntity> findByReceiverIdOrderByIdDesc(
            Long receiverId, Pageable pageable);

    List<NotificationJpaEntity> findByReceiverIdAndIdLessThanOrderByIdDesc(
            Long receiverId, Long cursorId, Pageable pageable);

    boolean existsByReceiverIdAndIdLessThan(Long receiverId, Long lastId);

    void deleteByRedirectUrlStartingWith(String urlPrefix);
}