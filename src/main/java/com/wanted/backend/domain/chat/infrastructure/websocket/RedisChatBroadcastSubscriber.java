package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

// 모든 인스턴스가 이 리스너를 통해 Redis 채널을 구독하고 있으므로, 어느 인스턴스가 발행했든
// 이 메서드는 전체 인스턴스에서 실행되어 각자의 로컬 STOMP 브로커로 전달한다.
@Component
public class RedisChatBroadcastSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisChatBroadcastSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatBroadcastSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        handleRawMessage(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    // Redis Message 목(mock)을 구성하지 않고도 역직렬화·로컬 전달 로직만 단위 테스트할 수 있도록 분리.
    void handleRawMessage(String rawJson) {
        try {
            ChatBroadcastEnvelope envelope = objectMapper.readValue(rawJson, ChatBroadcastEnvelope.class);
            Class<?> payloadType = Class.forName(envelope.payloadType());
            Object payload = objectMapper.readValue(envelope.payloadJson(), payloadType);
            messagingTemplate.convertAndSend(envelope.destination(), payload);
        } catch (Exception e) {
            log.error("Redis 채팅 브로드캐스트 메시지 처리 실패. raw={}", rawJson, e);
        }
    }
}
