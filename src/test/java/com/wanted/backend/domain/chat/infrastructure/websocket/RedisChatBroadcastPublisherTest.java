package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatBroadcastPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("broadcast하면 destination과 payload가 봉투에 담겨 chat-broadcast 채널로 발행된다")
    void broadcast_publishesEnvelopeToChannel() throws Exception {
        RedisChatBroadcastPublisher publisher = new RedisChatBroadcastPublisher(redisTemplate, objectMapper);
        ChatMessageEvent payload = new ChatMessageEvent("CHAT", 1L, 2L, "홍*동", "hi", LocalDateTime.now());

        publisher.broadcast("/sub/chat-rooms/45", payload);

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), bodyCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo(RedisChatBroadcastPublisher.CHANNEL);

        ChatBroadcastEnvelope envelope = objectMapper.readValue(bodyCaptor.getValue(), ChatBroadcastEnvelope.class);
        assertThat(envelope.destination()).isEqualTo("/sub/chat-rooms/45");
        assertThat(envelope.payloadType()).isEqualTo(ChatMessageEvent.class.getName());

        ChatMessageEvent decoded = objectMapper.readValue(envelope.payloadJson(), ChatMessageEvent.class);
        assertThat(decoded).isEqualTo(payload);
    }
}
