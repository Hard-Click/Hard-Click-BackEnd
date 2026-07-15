-- student_availability(V3.1.2, 종호)를 enrollment 단위 -> 학생(member) 단위로 전환한다.
--
-- 이유 1) 온보딩 시점에 enrollment 이 없다. 구독권 결제(OrderType.SUBSCRIPTION)는
--         subscribeUseCase 만 호출하고 enrollment 을 만들지 않는다(ConfirmOrderPaymentService).
--         결제완료 -> '스케줄 입력하기' 플로우에서 쓸 수 있는 키는 member_id 뿐이다.
-- 이유 2) '학생이 언제 시간이 되는가'는 코스와 무관한 학생 속성이다. 코스별로 두면 다중 코스
--         수강 시 같은 시간대가 코스 수만큼 중복 계상된다. V3.1.7 이 daily_cap_min 을 학생
--         단위로 옮긴 것과 같은 논거이며, CP-SAT 도 학생의 모든 활성 enrollment 를 합쳐 푼다.
--
-- 백필 없이 전환하는 근거: V3.1.2 이후 온보딩 기능이 미구현(JPA 엔티티 없음)이라 실데이터가 없다.
-- V3.1.8 이 daily_cap_min 을 백필 없이 제거할 때와 같은 판단.
--
-- ⚠️ 공용 테이블(스케줄러 입력) 변경 - DB_MIGRATION_RULES 7항에 따라 팀 채널 사전 공유 대상.
ALTER TABLE student_availability
    DROP KEY idx_avail_enrollment,
    CHANGE COLUMN enrollment_id member_id BIGINT NOT NULL COMMENT '학생 단위 - 코스와 무관',
    ADD KEY idx_avail_member (member_id);
