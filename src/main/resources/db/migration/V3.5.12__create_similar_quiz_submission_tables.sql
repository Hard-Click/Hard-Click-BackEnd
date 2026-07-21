-- 유사퀴즈(복습) 제출·답안 영속화.
-- 정규 퀴즈(quiz_submission/_answer)와 달리 UNIQUE(similar_quiz_id, member_id)를 두지 않는다:
-- 복습은 같은 학생이 여러 번 반복 응시하며(사다리 승급/강등 루프), 제출 이력이 시간순으로 쌓여야
-- 추천기(personalize)가 라운드 단위로 난이도·시간 신호를 판정할 수 있다.
-- 문항 정답/해설/난이도는 원문항(quiz_question)에서 조회하므로 비정규화하지 않는다.
-- time_spent_seconds: 문제별 풀이 체류 시간(초). 미측정은 NULL(0=진짜 0초와 구분 — FE 계약).

CREATE TABLE IF NOT EXISTS similar_quiz_submission (
    submission_id        BIGINT   NOT NULL AUTO_INCREMENT,
    similar_quiz_id      BIGINT   NOT NULL,
    member_id            BIGINT   NOT NULL,
    score                INT      NOT NULL,
    total_question_count INT      NOT NULL,
    correct_count        INT      NOT NULL,
    submitted_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (submission_id),
    KEY idx_sqs_member_id (member_id),
    KEY idx_sqs_similar_quiz_id (similar_quiz_id),
    CONSTRAINT fk_sqs_similar_quiz FOREIGN KEY (similar_quiz_id)
        REFERENCES similar_quiz (similar_quiz_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS similar_quiz_submission_answer (
    answer_id          BIGINT     NOT NULL AUTO_INCREMENT,
    submission_id      BIGINT     NOT NULL,
    question_id        BIGINT     NOT NULL,
    selected_index     INT,
    is_correct         TINYINT(1) NOT NULL DEFAULT 0,
    time_spent_seconds INT        NULL COMMENT '문제 풀이 체류 시간(초). NULL=미측정',
    PRIMARY KEY (answer_id),
    KEY idx_sqsa_submission_id (submission_id),
    CONSTRAINT fk_sqsa_submission FOREIGN KEY (submission_id)
        REFERENCES similar_quiz_submission (submission_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
