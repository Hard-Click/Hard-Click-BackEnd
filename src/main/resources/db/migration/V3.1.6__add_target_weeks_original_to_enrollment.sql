-- 설계 리뷰 반영: target_weeks 는 리플로우 시 갱신될 수 있으므로, 하드벽 계산의
-- 기준점은 등록 시점에 1회 저장 후 불변인 target_weeks_original 을 사용한다.
-- (하드벽 = min(target_weeks_original + 2주, 수능일). 갱신되는 target_weeks 기준으로
--  계산하면 리플로우마다 +2주가 밀려 사실상 무제한 슬립이 되므로 반드시 원본 기준.)
ALTER TABLE enrollment
    ADD COLUMN target_weeks_original INT NULL COMMENT 'CP-SAT: 등록 시점 목표 주수 스냅샷(불변) — 하드벽 계산 기준';
