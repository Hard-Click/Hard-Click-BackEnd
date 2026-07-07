-- Section E: 이탈 위험 (규칙 + Cox PH) (additive, 모두 신규)
-- 주의: 기존 V2 member_learning_profile.churn_risk 와 목적이 겹침.
--       V3 dropout_risk 를 정답 소스로 사용하고 churn_risk 는 legacy 취급(문서화 필요).

-- E-1) 이탈 위험 산출물 (rule / cox)
CREATE TABLE dropout_risk (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT   NOT NULL,
    computed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    risk_score    DOUBLE   NOT NULL,
    method        ENUM('RULE','COX') NOT NULL,
    recency_days  INT      NULL,
    miss_streak   INT      NULL,
    features      JSON     NULL,
    PRIMARY KEY (id),
    KEY idx_risk_enrollment (enrollment_id, computed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- E-2) Cox PH 종속변수 라벨 (생존분석). enrollment 당 1행.
CREATE TABLE dropout_event (
    id             BIGINT     NOT NULL AUTO_INCREMENT,
    enrollment_id  BIGINT     NOT NULL,
    event_occurred TINYINT(1) NOT NULL DEFAULT 0,
    event_date     DATE       NULL,
    censored       TINYINT(1) NOT NULL DEFAULT 0,
    observed_days  INT        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dropout_event_enrollment (enrollment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
