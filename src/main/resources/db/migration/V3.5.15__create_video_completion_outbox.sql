-- VideoCompletedEvent의 durable outbox. 영상 완료 트랜잭션과 '같은 커밋'으로 이 행을 기록해,
-- AFTER_COMMIT 리스너가 크래시로 실행되지 못하거나 다운스트림(Redis/DB) 일시장애로 유실되는 경우를 막는다.
-- 스케줄 relay가 due 행을 집어(FOR UPDATE SKIP LOCKED) 소비자에 재전달하고, 소비자는 멱등(processed_video_completion)이라
-- 재전달돼도 중복 집계되지 않는다. 성공하면 DONE, 실패하면 backoff 후 재시도, 최대 시도 초과 시 DEAD(운영 확인 대상).
--
-- 상태: PENDING(대기/재시도 예정) → PROCESSING(relay가 선점) → DONE(완료) | DEAD(최대 시도 초과).
CREATE TABLE video_completion_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    video_id        BIGINT       NOT NULL,
    course_id       BIGINT       NOT NULL,
    -- 이벤트 발생 시각(Instant)을 UTC datetime(6)으로 보관해 소비자가 원래 instant를 복원한다.
    occurred_at     datetime(6)  NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    -- 다음 시도 가능 시각. relay는 status='PENDING' AND next_attempt_at<=now 인 행만 집는다.
    next_attempt_at datetime(6)  NOT NULL,
    last_error      VARCHAR(500) NULL,
    created_at      datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at    datetime(6)  NULL,
    -- relay 폴링 조건(status, next_attempt_at)을 커버하는 인덱스.
    KEY idx_vco_status_next (status, next_attempt_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
