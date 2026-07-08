-- 설계 리뷰 반영: Frozen Zone 의 나머지 필드. locked(V3.1.9)와 함께 사용한다.
-- effective_from = 이 주간 스케줄 버전이 실제로 적용되기 시작하는 날짜.
-- 리플로우로 같은 week_no 에 새 버전이 생겨도, effective_from 이전 확정분은 그대로 두고
-- 이 날짜부터 새 스케줄을 적용해 '이미 지나간/확정된 날'이 덮이지 않도록 한다.
-- (조회 시: 활성 버전 = effective_from <= 오늘 인 것 중 최신)
ALTER TABLE weekly_schedule
    ADD COLUMN effective_from DATE NULL COMMENT 'Frozen Zone: 이 스케줄 버전 적용 시작일';
