package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatBroadcastPublisher implements ChatBroadcastPort {

    static final String CHANNEL = "chat-broadcast";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatBroadcastPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void broadcast(String destination, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            ChatBroadcastEnvelope envelope = new ChatBroadcastEnvelope(destination, payload.getClass().getName(), payloadJson);
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 브로드캐스트 직렬화 실패. destination=" + destination, e);
        }
    }
}
