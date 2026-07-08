-- 설계 리뷰 반영: 모의고사 등급 기반 다중코스 예산분배용 진단 점수 테이블.
-- CP-SAT 이 학생의 여러 활성 코스에 학습 시간(예산)을 나눌 때, 코스별 등급(1~9)을
-- 가중치로 사용한다. (예: 약한 코스에 시간을 더 배분)
-- member_id / course_id 는 cross-domain 참조 — FK 없이 컬럼+인덱스만 둔다 (ARCHITECTURE.md 패턴).
CREATE TABLE IF NOT EXISTS student_diagnostic_score (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    course_id  BIGINT   NOT NULL,
    grade      TINYINT  NOT NULL COMMENT '모의고사 등급 (1=최상 ... 9=최하)',
    exam_date  DATE     NOT NULL COMMENT '응시일 — 같은 코스라도 시점별로 여러 행 허용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 같은 학생·코스·응시일 중복 방지. (member_id, course_id) leftmost prefix 로 학생별 코스 조회도 커버.
    UNIQUE KEY uq_diag_member_course_date (member_id, course_id, exam_date),
    -- 등급은 수능 표준 9등급만 허용 (MySQL 8.0.16+ 에서 강제).
    CONSTRAINT chk_diag_grade CHECK (grade BETWEEN 1 AND 9)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
