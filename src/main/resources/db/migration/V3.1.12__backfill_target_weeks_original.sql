-- 설계 리뷰 반영: V3.1.6 에서 추가한 target_weeks_original 스냅샷 백필.
-- V3.1.6 은 이미 적용된 마이그레이션이라 수정 불가(체크섬 불변) → 백필은 별도 버전으로 분리한다.
-- 리플로우 미구현 시점이므로 현재 target_weeks 값이 곧 등록 시점 원본이다.
-- 이미 목표 주수가 세팅된 기존 행만 스냅샷을 채운다 (미온보딩 행은 CP-SAT 비대상이므로 NULL 유지).
UPDATE enrollment
SET target_weeks_original = target_weeks
WHERE target_weeks IS NOT NULL
  AND target_weeks_original IS NULL;
