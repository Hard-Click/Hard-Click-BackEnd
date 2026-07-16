package com.wanted.backend.domain.chat.infrastructure.websocket;

// Redis Pub/Sub 채널로 실어보내는 봉투. payload는 ChatMessageEvent, PresenceUpdateMessage 등
// 서로 다른 타입이 섞여 들어오므로, 구독 측에서 원래 타입으로 역직렬화할 수 있도록
// FQCN을 같이 실어보낸다(다형 타입 메타데이터를 JSON 본문에 섞는 대신 명시적 필드로 분리).
record ChatBroadcastEnvelope(String destination, String payloadType, String payloadJson) {
}
