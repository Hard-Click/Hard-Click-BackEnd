-- 채팅 메시지 히스토리. chat_room이 같은 도메인 내 상위 애그리거트라 FK를 건다
-- (study 등 타 도메인 참조와 달리 동일 도메인 내부 관계이므로 V3.4.1의 chat_room_participant와 동일 패턴).

CREATE TABLE chat_message (
    chat_message_id BIGINT   NOT NULL AUTO_INCREMENT,
    chat_room_id    BIGINT   NOT NULL,
    sender_id       BIGINT   NOT NULL,
    content         TEXT     NOT NULL,
    sent_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_message_id),
    KEY idx_chat_message_room_sent (chat_room_id, sent_at),
    CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (chat_room_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
