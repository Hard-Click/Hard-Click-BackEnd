# CLAUDE.md — Flown Backend

Spring Boot 3.5.14 · Java 17 · 클린 아키텍처(DDD) 유료 강의 플랫폼.
매 세션 자동 로드. 팀 규칙 문서·코드에 이미 있는 건 여기 안 적고, 없는 것만 둔다.

## 팀 규칙 (이미 있는 문서 — 반드시 준수)
- 브랜치·PR·커밋·CI/CD·Secrets·인프라 스펙 → `docs/DEV_RULES.md`
- 작업 워크플로(이슈 먼저·1 Feature 1 PR·테스트 필수·Closes #) → `docs/WORKFLOW.md`

## AI 작업 원칙
- 코딩·리뷰·리팩터 시 **karpathy-guidelines 스킬 준수** (설치 필수 — 온보딩 참고)
- 새 유틸 만들기 전 `global/common` 검색 (중복 금지)

## AI가 코드만 봐선 모르는 것 (← 이 파일의 핵심)
- **N+1 결정 순서**: 단일→fetch join / 다중→batch size / 집계→비정규화 (`open-in-view:false`)
- **의도적 결정 되돌리지 마**: 비정규화 count 등 성능상 일부러 → `docs/ARCHITECTURE.md`(ADR)
- **명세 고정값 임의 변경 금지**: 비번 N회·인증 5분·구독료=(수능일까지 일수)×30000 → `docs/DOMAIN_RULES.md`
- **스키마 변경 = `db/migration/V___.sql` 마이그레이션 + CI 드리프트 게이트 통과** (임의 스키마 변경 금지)
- 도메인 특수 규칙 → `src/main/java/com/wanted/backend/domain/{payment,study_timer,community}/CLAUDE.md`

## 에러 처리
- 성공 응답 `ApiResponse`, 예외 `BusinessException(ErrorCode.X)`, 새 ErrorCode는 접두어 규칙 준수 → `docs/ERROR_HANDLING.md`
