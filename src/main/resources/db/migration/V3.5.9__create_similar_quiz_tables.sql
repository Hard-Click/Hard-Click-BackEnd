-- 오답 기반 유사퀴즈 생성 세트 영속화.
-- 생성(①) 시점의 문항 구성을 고정해 제출(②) 채점 시 동일 세트로 재현한다.
-- 재응시 없음: 진입마다 새 세트를 생성하므로 (member, course, week) 유니크 제약은 두지 않는다.
-- 문항은 기존 quiz_question 행을 참조(question_id)하므로 정답/해설은 원문항에서 조회한다(비정규화 X).

CREATE TABLE IF NOT EXISTS similar_quiz (
    similar_quiz_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL,
    course_id       BIGINT       NOT NULL,
    week_number     INT,
    title           VARCHAR(255) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (similar_quiz_id),
    KEY idx_similar_quiz_member_id (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS similar_quiz_question (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    similar_quiz_id BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    question_order  INT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sqq_similar_quiz_id (similar_quiz_id),
    CONSTRAINT fk_sqq_similar_quiz FOREIGN KEY (similar_quiz_id)
        REFERENCES similar_quiz (similar_quiz_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
