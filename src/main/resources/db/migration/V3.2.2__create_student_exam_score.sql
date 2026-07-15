-- 온보딩 3단계(최근 모의고사 성적) 입력값 - 수능 응시영역 단위로 원점수를 받는다.
--
-- student_diagnostic_score(V3.1.10, 종호)와의 관계:
--   저쪽은 CP-SAT 이 코스별 예산을 나눌 때 쓰는 '코스 단위 등급' 테이블이다(course_id NOT NULL,
--   UNIQUE(member_id, course_id, exam_date)). 온보딩 시점엔 구독만 있고 수강 코스가 없어서
--   course_id 를 채울 수 없고, 응시영역(국어/수학/...)->course 매핑도 아직 없다.
--   저 테이블의 course_id 를 nullable 로 바꾸면 UNIQUE 가 NULL 중복을 허용해 중복 방지가 깨지므로
--   건드리지 않고, 온보딩 원본은 이 테이블에 영역 단위로 보관한다.
--   -> 영역->course 매핑이 생기면 이 테이블을 소스로 student_diagnostic_score 를 채운다(별도 이슈).
--
-- raw_score 를 함께 보관하는 이유: 등급컷은 시행마다 바뀌므로 원점수를 남겨둬야 재계산이 가능하다.
-- member_id 는 cross-domain 참조 - FK 없이 컬럼만 둔다 (ARCHITECTURE.md 패턴).
CREATE TABLE IF NOT EXISTS student_exam_score (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    member_id    BIGINT      NOT NULL,
    subject_area VARCHAR(20) NOT NULL COMMENT '응시영역: KOREAN/MATH/ENGLISH/HISTORY/EXPLORATION_1/EXPLORATION_2',
    subject_name VARCHAR(40) NULL COMMENT '응시과목 - 영역 내 선택과목명(영어/한국사는 선택 없음 -> NULL)',
    raw_score    SMALLINT    NOT NULL COMMENT '원점수(0~100) - 등급 재계산용 원본',
    grade        TINYINT     NOT NULL COMMENT '원점수에서 변환된 등급 (1=최상 ... 9=최하)',
    exam_date    DATE        NOT NULL COMMENT '응시일 - 온보딩 입력 시점엔 입력일로 대체',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 같은 학생·영역·응시일 중복 방지. (member_id) leftmost prefix 로 학생별 조회도 커버.
    UNIQUE KEY uq_exam_member_area_date (member_id, subject_area, exam_date),
    CONSTRAINT chk_exam_grade CHECK (grade BETWEEN 1 AND 9),
    CONSTRAINT chk_exam_raw_score CHECK (raw_score BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
