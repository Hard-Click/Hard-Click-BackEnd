-- 휴식 요일을 학생 단위(student_capacity, V3.1.7)로 옮긴다.
--
-- enrollment_onboarding(V3.1.2)에 rest_days 가 있으나 그 테이블은 enrollment 단위라
-- 온보딩 시점(구독만 있고 수강 코스 없음)에 쓸 수 없다. 쉬는 날은 daily_cap_min 과 마찬가지로
-- 코스가 아니라 학생의 속성이므로 student_capacity 에 함께 둔다.
-- -> 이 마이그레이션 이후 enrollment_onboarding 은 쓰이지 않는다(테이블은 규칙상 남겨둔다).
--    Python 스케줄러의 MySQLStudentCapRepository 는 아직 enrollment_onboarding 을 읽으므로
--    student_capacity 기준으로 함께 고쳐야 한다(별도 이슈).
--
-- onboarded_at 은 온보딩 완료 여부의 단일 기준이다(NULL = 미완료). 스케줄러의 콜드스타트
-- 폴백(주 420분) 진입 조건도 이 값으로 판단한다.
--
-- ⚠️ 공용 테이블(스케줄러 입력) 변경 - DB_MIGRATION_RULES 7항에 따라 팀 채널 사전 공유 대상.
ALTER TABLE student_capacity
    ADD COLUMN rest_days    INT      NOT NULL DEFAULT 0 COMMENT '휴식 요일 비트마스크 (bit0=일 ... bit6=토)',
    ADD COLUMN onboarded_at DATETIME NULL COMMENT '온보딩 완료 시각 - NULL 이면 미완료';
