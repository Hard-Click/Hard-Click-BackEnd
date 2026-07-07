-- 설계 리뷰 반영: daily_cap_min 은 학생이 하루에 감당 가능한 '총량'이므로
-- 코스(enrollment_onboarding)가 아닌 학생 단위로 관리한다. 코스별로 두면 다중 코스
-- 동시수강 시 cap 이 코스 수만큼 뚫린다 (코스별 CP-SAT 이 따로 풀리면 하루 총량을 아무도 안 봄).
-- CP-SAT 은 학생의 모든 활성 enrollment 를 합쳐 이 cap 안에서 한 번에 풀어야 한다.
-- 기존 enrollment_onboarding.daily_cap_min 은 V3.1.8 에서 제거한다.
-- student_id 는 member 참조 — cross-domain 이라 FK 없이 컬럼만 둔다 (ARCHITECTURE.md 패턴).
CREATE TABLE IF NOT EXISTS student_capacity (
    student_id    BIGINT   NOT NULL,
    daily_cap_min INT      NULL COMMENT '일일 학습 상한(분) — 학생 단위 총량',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
