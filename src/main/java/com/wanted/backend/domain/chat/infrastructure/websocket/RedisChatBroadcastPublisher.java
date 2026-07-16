package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 모든 ChatBroadcastPort 호출(7곳)이 이 한 곳을 거치므로, Redis 인프라 장애를 여기서 한 번만
// 삼키면 각 호출부(리스너/서비스)가 저마다 try/catch를 중복해서 두지 않아도 된다.
@Component
public class RedisChatBroadcastPublisher implements ChatBroadcastPort {

    private static final Logger log = LoggerFactory.getLogger(RedisChatBroadcastPublisher.class);

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
        } catch (DataAccessException e) {
            log.error("Redis 채팅 브로드캐스트 발행 실패. destination={}", destination, e);
        }
    }
}
