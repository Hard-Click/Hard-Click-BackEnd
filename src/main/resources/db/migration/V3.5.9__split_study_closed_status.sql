-- #586: study.status의 CLOSED가 "정원 마감"과 "방장 해산(방폭)" 두 의미로 겹쳐 있어
-- 목록에서 해산된 스터디만 골라 숨길 수 없던 문제 — FULL / DISSOLVED로 분리한다.
--
-- 기존 CLOSED row는 기계적으로 구분 가능:
--   * 정원 마감: join()이 정원이 찰 때만 CLOSED로 바꿨으므로 current_count >= max_count
--   * 방장 해산: validateDeletable()이 "혼자일 때만" 허용하므로 current_count(=1) < max_count(>=2)
--     (방장 단독 leave로 count=0이 된 경우도 여기에 포함)
-- DDL 변경 없음 (status VARCHAR(20)에 'DISSOLVED' 수용) — 데이터 UPDATE만 수행.

UPDATE study
SET status = 'FULL'
WHERE status = 'CLOSED'
  AND current_count >= max_count;

UPDATE study
SET status = 'DISSOLVED'
WHERE status = 'CLOSED';
