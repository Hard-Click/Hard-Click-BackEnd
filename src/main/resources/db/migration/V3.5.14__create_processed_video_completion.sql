-- VideoCompletedEvent 소비자(랭킹·수강량 잔디)의 멱등 처리용 dedup 테이블.
-- 레슨 완료는 '회원당 영상당 1회' 집계가 도메인상 맞다. 각 소비자는 증가 전에
-- (consumer_id, member_id, video_id)를 원자적으로 선점(INSERT)하고, 이미 처리됐으면(중복키) 증가를 건너뛴다.
-- 동시 최초완료 경합·이벤트 재전달로 인한 '이벤트당 +1' 중복 집계를 차단한다.
--
-- consumer_id를 키에 포함하는 이유: 랭킹(Redis)과 잔디(DB)는 서로 다른 저장소라 각자 독립적으로
-- 한 번씩 처리돼야 한다. 소비자별로 선점 행이 분리돼야 한쪽 처리가 다른 쪽을 막지 않는다.
--
-- 한계(별도 후속 #657 2b·outbox): 선점 커밋 후 증가가 유실되면(크래시·Redis 장애) 재처리가 막혀
-- under-count가 남을 수 있다. at-least-once 내구성은 durable outbox+재시도로 별도 해결한다.
CREATE TABLE processed_video_completion (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    consumer_id  VARCHAR(64)  NOT NULL,
    member_id    BIGINT       NOT NULL,
    video_id     BIGINT       NOT NULL,
    -- Hibernate 6는 LocalDateTime을 datetime(6)으로 매핑한다. 매핑 엔티티는 없지만 정밀도 관례를 맞춘다.
    processed_at datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_pvc_consumer_member_video (consumer_id, member_id, video_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
