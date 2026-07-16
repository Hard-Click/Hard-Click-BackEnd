package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatBroadcastSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("정상적인 봉투를 받으면 원래 타입으로 역직렬화해서 로컬 브로커로 전달한다")
    void handleRawMessage_deliversDecodedPayloadLocally() throws Exception {
        RedisChatBroadcastSubscriber subscriber = new RedisChatBroadcastSubscriber(messagingTemplate, objectMapper);
        ChatMessageEvent payload = new ChatMessageEvent("CHAT", 1L, 2L, "홍*동", "hi", LocalDateTime.now());
        String envelopeJson = objectMapper.writeValueAsString(
                new ChatBroadcastEnvelope("/sub/chat-rooms/45", ChatMessageEvent.class.getName(),
                        objectMapper.writeValueAsString(payload)));

        subscriber.handleRawMessage(envelopeJson);

        verify(messagingTemplate).convertAndSend("/sub/chat-rooms/45", payload);
    }

    @Test
    @DisplayName("깨진 메시지를 받아도 예외를 삼키고 로컬 브로커로 아무것도 보내지 않는다")
    void handleRawMessage_malformedJson_swallowsException() {
        RedisChatBroadcastSubscriber subscriber = new RedisChatBroadcastSubscriber(messagingTemplate, objectMapper);

        subscriber.handleRawMessage("이건 JSON이 아니다");

        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    @DisplayName("존재하지 않는 payloadType이면 예외를 삼키고 로컬 브로커로 아무것도 보내지 않는다")
    void handleRawMessage_unknownPayloadType_swallowsException() throws Exception {
        RedisChatBroadcastSubscriber subscriber = new RedisChatBroadcastSubscriber(messagingTemplate, objectMapper);
        String envelopeJson = objectMapper.writeValueAsString(
                new ChatBroadcastEnvelope("/sub/chat-rooms/45", "com.wanted.backend.NoSuchClass", "{}"));

        subscriber.handleRawMessage(envelopeJson);

        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
