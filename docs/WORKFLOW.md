# 작업 워크플로우 (일정 관리 & 기능 개발 원칙)

> 원본 "백엔드 코드컨벤션(워크플로)" 기준. 모든 팀원(백/프론트) 개발 착수 전 숙지·준수.

## 핵심 원칙
1. **기능(`feature`) 이슈는 착수 전 미리 전부 등록** — 담당 기능 이슈를 개발 시작 전 일괄 등록, 마일스톤 연동.
2. **기타 이슈(`fix`·`refactor`·`docs`·`perf`)는 그때그때 생성**.
3. **기능 개발 시 테스트 코드 작성 필수** — 정상 동작 + 예외 처리 테스트 작성하고 로컬 100% 통과 후 push.
4. **1 Feature = 1 PR** — 여러 기능을 한 PR에 묶으면 반려.

## 5단계 워크플로우
1. **이슈 확인·프로젝트 연동** — Assignees·Labels·Milestone·Projects 연결 확인 → 보드 Todo
2. **상태 변경·브랜치 생성** — 카드 In Progress로 이동
   ```
   git checkout develop && git pull origin develop
   git checkout -b feature/#이슈번호-기능명
   ```
3. **구현·테스트 검증 후 push**
   ```
   git commit -m "feat: 기능 설명 (#12)"
   git push origin feature/#12-기능명
   ```
4. **develop 대상 PR 생성** — 본문 최상단에 `Closes #이슈번호` 필수
5. **리뷰 승인·머지** — 리더 Approve → 머지 시 이슈 자동 Close + 마일스톤 진행률 갱신 + 보드 Done 자동 이동
