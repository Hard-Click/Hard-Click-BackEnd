-- Python-Server(스케줄러 AI)의 A/B 실험 관측 로그 테이블.
-- 스케줄 결과엔 영향 없는 관측 전용 테이블이라, 여태 이 테이블이 없어도
-- Python 쪽 infrastructure/repositories.py의 fail-soft(에러 1146/1054 스킵)로
-- 500은 안 났지만, 로그가 전혀 안 쌓여 A/B 분석(calibrate_policy_constants.py)이
-- 불가능한 상태였다. 정식 집계를 위해 추가.

-- 실험 노출 로그: 회원이 어떤 실험의 어떤 variant를 받았는지 매 스케줄 생성(weekly_reflow·
-- generate-for-member)마다 기록하는 시계열 로그 — 회원당 여러 행이 정상(1회성 아님).
CREATE TABLE experiment_exposure (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL,
    experiment_name VARCHAR(255) NOT NULL,
    variant         VARCHAR(255) NOT NULL,
    exposed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exposure_member_experiment (member_id, experiment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- shadow mode 결정 로그: 실제 스케줄엔 반영하지 않고 "만약 이 variant였다면" 델타만 관측
CREATE TABLE experiment_shadow_decision (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    member_id                BIGINT       NOT NULL,
    experiment_name          VARCHAR(255) NOT NULL,
    variant                  VARCHAR(255) NOT NULL,
    extension_delta          INT          NULL,
    weekly_minutes_delta     INT          NULL,
    schedule_would_change    TINYINT(1)   NOT NULL DEFAULT 0,
    detail                   JSON         NOT NULL,
    logged_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_shadow_member_experiment (member_id, experiment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
