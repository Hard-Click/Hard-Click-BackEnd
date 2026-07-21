package com.wanted.backend.domain.notification.infrastructure.sse;

import com.wanted.backend.domain.notification.application.dto.NotificationPayload;
import com.wanted.backend.domain.notification.application.port.NotificationSsePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class NotificationSseAdapter implements NotificationSsePort {

    // 30분 타임아웃 (Nginx 프록시 타임아웃보다 짧게 설정)
    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final String EVENT_NAME = "notification";
    // 최초 연결 시 전송하는 더미 이벤트 (연결 확립용 - 브라우저 EventSource는 첫 데이터 수신 전까지 연결 미확인)
    private static final String CONNECT_EVENT_NAME = "connect";
    // ALB idle timeout(기본 60초) 이전에 트래픽을 흘려 커넥션 유지용 (스케줄러가 주기 전송)
    private static final String HEARTBEAT_EVENT_NAME = "heartbeat";
    private static final String HEARTBEAT_DATA = "ping";

    // 멤버 1명이 복수 탭/기기에서 연결할 수 있으므로 List로 관리
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter connect(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        emitters.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료 시 자동 제거
        emitter.onCompletion(() -> remove(memberId, emitter));
        emitter.onTimeout(() -> remove(memberId, emitter));
        emitter.onError(e -> remove(memberId, emitter));

        // 연결 즉시 더미 이벤트 전송 (없으면 브라우저가 연결을 인식 못함)
        sendToEmitter(memberId, emitter, CONNECT_EVENT_NAME, "connected");

        log.debug("[SSE] connected memberId={}", memberId);
        return emitter;
    }

    @Override
    public void send(Long memberId, NotificationPayload payload) {
        List<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null || memberEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : memberEmitters) {
            sendToEmitter(memberId, emitter, EVENT_NAME, payload);
        }
    }

    @Override
    public void sendHeartbeat() {
        for (Map.Entry<Long, List<SseEmitter>> entry : emitters.entrySet()) {
            Long memberId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                sendToEmitter(memberId, emitter, HEARTBEAT_EVENT_NAME, HEARTBEAT_DATA);
            }
        }
    }


    private void sendToEmitter(Long memberId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException e) {
            // 전송 실패 = 연결이 이미 끊김 → 제거
            remove(memberId, emitter);
            log.debug("[SSE] emitter removed on send failure, memberId={}", memberId);
        }
    }

    private void remove(Long memberId, SseEmitter emitter) {
        emitters.computeIfPresent(memberId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}