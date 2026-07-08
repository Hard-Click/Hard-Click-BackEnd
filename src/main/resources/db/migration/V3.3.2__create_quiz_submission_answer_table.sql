-- 퀴즈 제출의 문항별 답안. 학생이 고른 보기와 정오답 여부를 저장한다.
-- MySQL DDL 롤백 불가 원칙에 따라 submission 테이블(V3.3.1)과 파일을 분리한다.
-- selected_option_id는 미응답(무응답 제출) 허용을 위해 NULL 가능.

CREATE TABLE IF NOT EXISTS quiz_submission_answer (
    answer_id          BIGINT     NOT NULL AUTO_INCREMENT,
    submission_id      BIGINT     NOT NULL,
    question_id        BIGINT     NOT NULL,
    selected_option_id BIGINT,
    is_correct         TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (answer_id),
    KEY idx_qsa_submission_id (submission_id),
    UNIQUE KEY uq_qsa_submission_question (submission_id, question_id),
    CONSTRAINT fk_qsa_submission FOREIGN KEY (submission_id) REFERENCES quiz_submission (submission_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
