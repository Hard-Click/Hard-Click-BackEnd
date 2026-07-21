-- 지난 미완료 슬롯 MISSED 전이 배치(markMissedBefore)가
-- WHERE status = 'PLANNED' AND plan_date < :today 로 schedule_slot 전체를 스캔한다.
-- 등호 조건(status) 먼저, range 조건(plan_date)을 뒤에 두는 순서로 복합 인덱스를 추가한다.
ALTER TABLE schedule_slot
    ADD INDEX idx_slot_status_plan_date (status, plan_date);
