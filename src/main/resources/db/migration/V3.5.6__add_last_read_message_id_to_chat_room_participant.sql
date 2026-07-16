-- notice_read_status(공지×유저 fan-out row)를 채팅에 그대로 쓰면 "메시지 수 × 참여자 수"로
-- 무한정 늘어나므로, 참여자당 "마지막으로 읽은 메시지 ID" 1개만 저장하는 방식(카카오톡 방식)으로
-- 대체한다. 저장 공간이 O(메시지 × 참여자) → O(참여자)로 고정된다.
-- 아직 아무 메시지도 읽지 않은 참여자를 위해 NULL을 허용한다.
ALTER TABLE chat_room_participant
    ADD COLUMN last_read_message_id BIGINT NULL AFTER member_id;
