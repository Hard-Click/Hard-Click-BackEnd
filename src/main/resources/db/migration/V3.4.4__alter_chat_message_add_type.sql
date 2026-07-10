-- 채팅 메시지 히스토리에 입장/퇴장 시스템 메시지도 함께 저장하기 위해
-- type 컬럼을 추가하고, 시스템 메시지는 발신자가 없으므로 sender_id를 nullable로 변경한다.

ALTER TABLE chat_message
    MODIFY COLUMN sender_id BIGINT NULL,
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'CHAT';
