-- Section C: CP-SAT 출력 + 달성 기록 (additive, 모두 신규)
-- 주의: 기존 V2 generated_schedule 및 V1 daily_study_stats 와 목적이 겹치므로,
--       V3 산출물을 '정답 소스'로 사용하고 기존 테이블은 legacy 로 취급한다(문서화 필요).

-- C-1) 주간 스케줄 스냅샷 (리플로우 이력)
CREATE TABLE weekly_schedule (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT       NOT NULL,
    week_no       INT          NOT NULL,
    generated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reflow_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_weekly_enrollment (enrollment_id, week_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- C-2) 배치된 슬롯 (weekly_schedule 자식 → 같은 도메인이라 FK 사용)
CREATE TABLE schedule_slot (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    weekly_schedule_id BIGINT NOT NULL,
    lesson_id          BIGINT NOT NULL,
    plan_date          DATE   NOT NULL,
    start_time         TIME   NULL,
    planned_min        INT    NOT NULL DEFAULT 0,
    status             ENUM('PLANNED','DONE','MISSED') NOT NULL DEFAULT 'PLANNED',
    PRIMARY KEY (id),
    KEY idx_slot_schedule (weekly_schedule_id),
    KEY idx_slot_lesson (lesson_id),
    CONSTRAINT fk_slot_weekly FOREIGN KEY (weekly_schedule_id)
        REFERENCES weekly_schedule (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- C-3) 일별 달성 기록 (스트릭 산출 원천). 'date' 는 예약어라 achieved_date 로 명명.
CREATE TABLE daily_achievement (
    id            BIGINT     NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT     NOT NULL,
    achieved_date DATE       NOT NULL,
    planned_min   INT        NOT NULL DEFAULT 0,
    actual_min    INT        NOT NULL DEFAULT 0,
    achieved      TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_daily_ach (enrollment_id, achieved_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
