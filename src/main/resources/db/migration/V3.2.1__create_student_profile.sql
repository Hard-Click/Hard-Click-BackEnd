-- 온보딩 1단계(학습 스케줄 초기 설정) 입력값 - 학생 단위 1:1.
-- 구독 결제 직후 입력받으므로 이 시점엔 enrollment 이 없다(ConfirmOrderPaymentService 는
-- OrderType.SUBSCRIPTION 일 때 enrollment 을 생성하지 않는다) -> member_id 로 키를 잡는다.
-- member_id 는 cross-domain 참조 - FK 없이 컬럼만 둔다 (ARCHITECTURE.md 패턴).
--
-- ⚠️ 이 테이블의 값은 현재 CP-SAT 스케줄러가 읽지 않는다. 목표/전략/선택과목은 향후
--    응시영역->course 매핑과 코스 추천에 쓸 입력으로 먼저 수집만 한다.
CREATE TABLE IF NOT EXISTS student_profile (
    member_id             BIGINT      NOT NULL,
    target_university     VARCHAR(100) NULL COMMENT '목표 대학 - 자유 입력',
    target_major          VARCHAR(100) NULL COMMENT '목표 학과 - 자유 입력',
    admission_strategy    VARCHAR(20) NOT NULL COMMENT '입시 전략: REGULAR(정시 위주) / EARLY(수시 위주) / UNDECIDED(병행·미정)',
    korean_elective       VARCHAR(30) NULL COMMENT '국어 선택과목: SPEECH_WRITING(화법과 작문) / LANGUAGE_MEDIA(언어와 매체)',
    math_elective         VARCHAR(30) NULL COMMENT '수학 선택과목: CALCULUS(미적분) / GEOMETRY(기하) / STATISTICS(확률과 통계)',
    exploration_track     VARCHAR(20) NULL COMMENT '탐구 계열: SOCIAL(사회탐구) / SCIENCE(과학탐구) / MIXED(혼합)',
    exploration_subject_1 VARCHAR(40) NULL COMMENT '탐구 과목 1',
    exploration_subject_2 VARCHAR(40) NULL COMMENT '탐구 과목 2',
    second_language       TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '제2외국어/한문 응시 여부',
    study_preference      VARCHAR(20) NOT NULL COMMENT '학습 성향: MORNING(아침형) / EVENING(저녁형) / NONE(무관)',
    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
