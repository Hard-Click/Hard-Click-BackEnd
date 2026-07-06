# study_timer 도메인 규칙

- **동시성 = MySQL 행 락**: 회원 단위 `SELECT ... FOR UPDATE` (MemberRowLockAdapter),
  타임아웃 3초. (Redis 분산락 아님 — 혼동 주의)
- **세션 상태 전이**: RUNNING → PAUSED → ENDED / CANCELED. 잘못된 전이 방지 로직 유지.
- **누적 시간**: `accumulated_study_seconds` 계산 로직 임의 변경 금지 (잔디·랭킹 집계 근거).
