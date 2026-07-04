-- Module 05: AI 학습 시간표 최적화용 신규 테이블
-- 기존 스키마는 Flyway baseline(V1)로 마킹되고, 여기서부터 버전 관리 시작

-- ─────────────────────────────────────────────
-- 선생님 학습 기준 (최적화 제약 입력)
-- ─────────────────────────────────────────────
CREATE TABLE course_learning_policy (
    course_id                 BIGINT       NOT NULL PRIMARY KEY,
    target_end_date           DATE,
    daily_recommended_minutes INT,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lesson_learning_policy (
    lesson_id              BIGINT NOT NULL PRIMARY KEY,
    review_interval_days   INT,
    prerequisite_lesson_id BIGINT,
    difficulty_weight      DOUBLE,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ─────────────────────────────────────────────
-- ETL 산출 피처 (배치가 채움, 읽기 전용)
-- ─────────────────────────────────────────────
CREATE TABLE member_learning_profile (
    member_id       BIGINT NOT NULL PRIMARY KEY,
    pace_multiplier DOUBLE,
    churn_risk      DOUBLE,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE member_slot_adherence (
    id             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT NOT NULL,
    day_of_week    TINYINT NOT NULL,
    hour           TINYINT NOT NULL,
    adherence_rate DOUBLE,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_slot (member_id, day_of_week, hour)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE member_lesson_stat (
    id                    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id             BIGINT NOT NULL,
    lesson_id             BIGINT NOT NULL,
    actual_completion_sec INT,
    rewatch_count         INT,
    last_studied_at       DATETIME,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_lesson (member_id, lesson_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ─────────────────────────────────────────────
-- 생성된 학습 시간표 (최적화 결과)
-- ─────────────────────────────────────────────
CREATE TABLE generated_schedule (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    slot          VARCHAR(20),
    lesson_id     BIGINT,
    session_type  VARCHAR(20),          -- 학습 / 복습
    reason_text   VARCHAR(500),         -- LLM 설명
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_date (member_id, schedule_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
