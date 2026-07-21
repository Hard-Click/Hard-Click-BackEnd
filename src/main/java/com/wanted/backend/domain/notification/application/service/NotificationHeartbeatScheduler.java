package com.wanted.backend.domain.notification.application.service;

import com.wanted.backend.domain.notification.application.port.NotificationSsePort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

    private final NotificationSsePort notificationSsePort;

    @Scheduled(fixedRateString = "${notification.sse.heartbeat-rate-ms:30000}")
    public void sendHeartbeat() {
        notificationSsePort.sendHeartbeat();
    }
}
