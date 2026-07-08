-- Section B: CP-SAT 입력 - 학생 온보딩/개인화 (additive)

-- B-1) enrollment: 목표 수강 주수 추가 (nullable, 이미 계획된 학생만 값 존재)
ALTER TABLE enrollment
    ADD COLUMN target_weeks INT NULL COMMENT 'CP-SAT: 목표 수강 주수(개인화)';

-- B-2) 온보딩 입력값 (enrollment 1:1)
CREATE TABLE IF NOT EXISTS enrollment_onboarding (
    enrollment_id BIGINT   NOT NULL,
    daily_cap_min INT      NULL COMMENT '일일 학습 상한(분)',
    rest_days     INT      NOT NULL DEFAULT 0 COMMENT '휴식 요일 비트마스크 (bit0=일 ... bit6=토)',
    onboarded_at  DATETIME NULL,
    PRIMARY KEY (enrollment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- B-3) 가용 시간 슬롯 (enrollment 1:N, 요일별 반복행)
CREATE TABLE IF NOT EXISTS student_availability (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT  NOT NULL,
    day_of_week   TINYINT NOT NULL COMMENT '0=일 ... 6=토',
    start_time    TIME    NOT NULL,
    end_time      TIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_avail_enrollment (enrollment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
