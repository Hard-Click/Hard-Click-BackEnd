-- Section A: CP-SAT 입력 - 강의/코스 제약 (additive)
-- 기존 테이블은 유지하고 컬럼만 추가한다. 계획서의 'lecture'는 실제 테이블 'lesson' 을 의미.
-- 참고: lesson.duration_seconds(예상시간), lesson.order_index(순서)는 이미 존재하므로 재사용한다.

-- A-1) course_learning_policy: CP-SAT 제약 컬럼 추가 (기존 target_end_date 는 삭제하지 않고 유지)
ALTER TABLE course_learning_policy
    ADD COLUMN recommended_duration_weeks INT                             NULL COMMENT 'CP-SAT: 권장 수강 주수',
    ADD COLUMN difficulty                 ENUM('EASY','MEDIUM','HARD')    NULL COMMENT 'CP-SAT: 코스 난이도',
    ADD COLUMN weekly_max_load_min        INT                             NULL COMMENT 'CP-SAT: 주당 학습 상한(분)';

-- A-2) lesson: 난이도만 추가 (예상시간=duration_seconds, 순서=order_index 재사용)
ALTER TABLE lesson
    ADD COLUMN difficulty ENUM('EASY','MEDIUM','HARD') NULL COMMENT 'CP-SAT: 강의 난이도';

-- A-3) 선수관계 N:N (기존 lesson_learning_policy.prerequisite_lesson_id 1:1 을 N:N 으로 확장)
--      cross-domain 참조는 FK 없이 컬럼+인덱스만 둔다 (ARCHITECTURE.md 패턴).
CREATE TABLE IF NOT EXISTS lesson_prerequisite (
    id                     BIGINT   NOT NULL AUTO_INCREMENT,
    lesson_id              BIGINT   NOT NULL,
    prerequisite_lesson_id BIGINT   NOT NULL,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lesson_prereq (lesson_id, prerequisite_lesson_id),
    KEY idx_lesson_prereq_lesson (lesson_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- A-4) 강의-퀴즈 N:N (quiz 테이블엔 lesson FK 없음 확인됨 → 매핑 테이블 필요)
CREATE TABLE IF NOT EXISTS lesson_quiz_map (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    quiz_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lesson_quiz (lesson_id, quiz_id),
    KEY idx_lesson_quiz_quiz (quiz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
