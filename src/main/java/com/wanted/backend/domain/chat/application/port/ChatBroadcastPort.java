package com.wanted.backend.domain.chat.application.port;

// enableSimpleBroker는 JVM 로컬 브로커라, ASG로 여러 인스턴스가 뜨면 한 인스턴스가 받은 메시지가
// 다른 인스턴스에 붙은 구독자한테는 전달되지 않는다. 이 포트는 브로드캐스트를 인스턴스 경계 밖으로
// 내보내는 지점을 도메인/애플리케이션 레이어에 추상화해, 구현체(Redis Pub/Sub 등)를 교체 가능하게 한다.
public interface ChatBroadcastPort {
    void broadcast(String destination, Object payload);
}
