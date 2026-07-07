-- Section D: FSRS 복습 (additive, 모두 신규)

-- D-1) 복습 카드 ((enrollment, lesson) 당 1장)
CREATE TABLE review_card (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    enrollment_id  BIGINT   NOT NULL,
    lesson_id      BIGINT   NOT NULL,
    stability      DOUBLE   NULL,
    difficulty     DOUBLE   NULL,
    due            DATETIME NULL,
    last_review    DATETIME NULL,
    state          ENUM('NEW','LEARNING','REVIEW','RELEARNING') NOT NULL DEFAULT 'NEW',
    reps           INT      NOT NULL DEFAULT 0,
    lapses         INT      NOT NULL DEFAULT 0,
    scheduled_days INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_card_enroll_lesson (enrollment_id, lesson_id),
    KEY idx_card_due (due)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- D-2) 복습 로그 (review_card 자식 → FK). 개인 가중치 재학습 원천.
CREATE TABLE review_log (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    card_id        BIGINT   NOT NULL,
    rating         TINYINT  NOT NULL COMMENT '1=again, 2=hard, 3=good, 4=easy',
    quiz_score     DOUBLE   NULL,
    reviewed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    elapsed_days   INT      NULL,
    scheduled_days INT      NULL,
    PRIMARY KEY (id),
    KEY idx_log_card (card_id),
    CONSTRAINT fk_log_card FOREIGN KEY (card_id)
        REFERENCES review_card (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- D-3) FSRS 파라미터. student 행이 없으면 global fallback(콜드스타트).
--      weights 는 FSRS 17개 가중치를 JSON 배열로 저장.
CREATE TABLE fsrs_params (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    scope            ENUM('GLOBAL','STUDENT') NOT NULL,
    student_id       BIGINT NULL,
    weights          JSON   NOT NULL COMMENT 'FSRS 17 weights (JSON array)',
    retention_target DOUBLE NOT NULL DEFAULT 0.9,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- NOTE: MySQL UNIQUE 는 NULL 을 중복 허용하므로, GLOBAL 유일성은 애플리케이션에서 보장할 것
    UNIQUE KEY uq_fsrs_scope_student (scope, student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
