-- 설계 리뷰 반영: Frozen Zone 플래그. locked=1 이면 리플로우 배치가 이 주를 절대
-- 재계산하지 않는다. 정책을 코드로만 지키면 배치 버그 시 확정된 주가 덮이므로,
-- 데이터로 강제하는 안전장치. (리플로우 대상 조회 시 WHERE locked = 0 필수)
ALTER TABLE weekly_schedule
    ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Frozen Zone: 1=리플로우 재계산 금지';
