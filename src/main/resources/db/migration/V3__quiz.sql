-- 강사 퀴즈 등록 (강의/섹션에 연결된 문항+보기).
-- course_id/section_id는 타 도메인(cource) 참조라 FK 없이 컬럼만 둔다
-- (cart_items.course_id와 동일한 Port + ReferenceEntity 패턴, ARCHITECTURE.md 참조).

CREATE TABLE quiz (
    quiz_id       BIGINT       NOT NULL AUTO_INCREMENT,
    course_id     BIGINT       NOT NULL,
    section_id    BIGINT       NOT NULL,
    instructor_id BIGINT       NOT NULL,
    title         VARCHAR(255) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quiz_id),
    KEY idx_quiz_course_section (course_id, section_id),
    KEY idx_quiz_instructor_id (instructor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE quiz_question (
    question_id     BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id         BIGINT NOT NULL,
    question_number INT    NOT NULL,
    question_text   TEXT   NOT NULL,
    explanation     TEXT,
    PRIMARY KEY (question_id),
    KEY idx_quiz_question_quiz_id (quiz_id),
    CONSTRAINT fk_quiz_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (quiz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE quiz_option (
    option_id     BIGINT       NOT NULL AUTO_INCREMENT,
    question_id   BIGINT       NOT NULL,
    option_number INT          NOT NULL,
    option_text   TEXT         NOT NULL,
    is_correct    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (option_id),
    KEY idx_quiz_option_question_id (question_id),
    CONSTRAINT fk_quiz_option_question FOREIGN KEY (question_id) REFERENCES quiz_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
