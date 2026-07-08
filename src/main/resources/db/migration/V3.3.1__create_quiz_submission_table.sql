-- 학생 퀴즈 제출(응시) 기록. 자동 채점 결과(점수/정답 수)를 저장한다.
-- quiz_id는 같은 quiz 도메인이라 FK를 걸지만, member_id는 타 도메인(identity) 참조라
-- FK 없이 컬럼만 둔다 (V3__quiz.sql의 course_id/instructor_id와 동일한 Port + Reference 패턴).
-- 1인 1제출 정책: (quiz_id, member_id) UNIQUE.

CREATE TABLE IF NOT EXISTS quiz_submission (
    submission_id        BIGINT   NOT NULL AUTO_INCREMENT,
    quiz_id              BIGINT   NOT NULL,
    member_id            BIGINT   NOT NULL,
    score                INT      NOT NULL,
    total_question_count INT      NOT NULL,
    correct_count        INT      NOT NULL,
    submitted_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (submission_id),
    UNIQUE KEY uq_quiz_submission_quiz_member (quiz_id, member_id),
    KEY idx_quiz_submission_member_id (member_id),
    CONSTRAINT fk_quiz_submission_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (quiz_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
