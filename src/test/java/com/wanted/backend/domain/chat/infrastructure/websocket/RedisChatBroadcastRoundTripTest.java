package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.PresenceUpdateMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

// publisher가 실어보낸 봉투를 subscriber가 그대로 복원할 수 있는지 확인한다 — 둘 중 하나만 바뀌어
// 봉투 계약이 어긋나면(필드명, 직렬화 방식 등) 이 테스트가 잡아낸다.
@ExtendWith(MockitoExtension.class)
class RedisChatBroadcastRoundTripTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("publisher가 발행한 원문을 subscriber에 그대로 넣으면 원래 payload가 로컬로 전달된다")
    void publishThenSubscribe_deliversOriginalPayload() {
        RedisChatBroadcastPublisher publisher = new RedisChatBroadcastPublisher(redisTemplate, objectMapper);
        RedisChatBroadcastSubscriber subscriber = new RedisChatBroadcastSubscriber(messagingTemplate, objectMapper);

        PresenceUpdateMessage payload = PresenceUpdateMessage.of(
                List.of(new ParticipantPresenceMessage(1L, "홍*동", true)));

        publisher.broadcast("/sub/chat-rooms/45", payload);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RedisChatBroadcastPublisher.CHANNEL), bodyCaptor.capture());

        subscriber.handleRawMessage(bodyCaptor.getValue());

        verify(messagingTemplate).convertAndSend("/sub/chat-rooms/45", payload);
    }
}
