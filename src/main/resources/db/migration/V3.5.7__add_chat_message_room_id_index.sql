-- unreadCount 계산(SELECT COUNT(*) FROM chat_message WHERE chat_room_id=? AND chat_message_id>?)이
-- chat_room_id 등호 + chat_message_id range 조건인데, 기존 idx_chat_message_room_sent는
-- (chat_room_id, sent_at) 순서라 id 기준 range를 인덱스로 못 태운다. 복합 인덱스를 추가한다.
ALTER TABLE chat_message
    ADD INDEX idx_chat_message_room_id (chat_room_id, chat_message_id);
