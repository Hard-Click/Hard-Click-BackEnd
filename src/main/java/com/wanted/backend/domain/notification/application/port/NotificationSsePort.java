package com.wanted.backend.domain.notification.application.port;

import com.wanted.backend.domain.notification.application.dto.NotificationPayload;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationSsePort {
    SseEmitter connect(Long memberId);
    void send(Long memberId, NotificationPayload payload);
    void sendHeartbeat();
}