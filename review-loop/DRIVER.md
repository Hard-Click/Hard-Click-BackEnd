# 리뷰 루프 · 드라이버(Claude Code) 플레이북

훅은 **순수 게이트**다. 수정·커밋·재push·예산은 **드라이버(Claude Code)가 대화 안에서** 수행한다.
훅/스크립트 안에서 `claude`를 부르지 않는다(중첩 금지).

## 역할 분리
| 주체 | 하는 일 | 안 하는 일 |
|---|---|---|
| pre-push 훅 | Gate1(ArchUnit)·Gate2(LLM Judge) 판정. Critical/미완성이면 exit 1. Minor는 요청서만 남기고 통과. | 코드 수정·커밋·push·claude 호출 |
| 드라이버(Claude Code) | 요청서를 읽어 사용자와 방안 확정 → Edit 수정 → 커밋 → 재push. 예산 통제. | Critical/미완성 임의 수정 |

## 트리거
- 사용자가 "push 해" → Claude가 `git push` 실행 → pre-push 훅이 게이트.
- 훅이 **Critical/미완성으로 exit 1** → push 중단. Claude는 사용자에게 보고하고 **사람 판단**을 받는다(자동수정 금지).
- 훅이 통과하며 **Minor 요청서**(`<git-dir>/review-fix-request.md`)를 남기면 → 아래 루프.

## 드라이버 루프 (예산 통제)
1. `<git-dir>/review-fix-request.md` 를 읽어 findings + 방안을 **사용자에게 채팅으로 제시**한다.
2. 사용자가 항목별 방안을 고른다(기본 1=추천, s=건너뛰기).
   - **s=건너뛰기(오탐)로 판정되면** → 오탐으로 기록(다음 판정이 같은 오탐을 반복하지 않게):
     `./gradlew reviewLesson --args="--rule <RULE> --kind FALSE_POSITIVE --note '<한 줄 근거>'"`
   - **방안을 골라 수정하면(=Judge가 옳았음)** → 확정으로 기록(오탐률의 분모를 채워 규칙 정확도 신호를 만듦):
     `./gradlew reviewLesson --args="--rule <RULE> --kind CONFIRMED --note '<무엇을 고쳤는지>'"`
   - 규칙 정확도 조회: `./gradlew reviewAccuracy` (오탐률 높은 규칙 = 프롬프트 개선 후보).
   - note에 특수문자(`—`·따옴표 등)가 있으면 `--note` 대신 `--note-file <UTF-8 경로>`로 — Windows argv 인코딩 깨짐 회피.
3. 확정된 방안대로 **Edit 도구로만** 수정한다. 나열된 항목 외 리팩터·무관 변경 금지.
4. `git diff` 를 사용자에게 보여주고 **커밋 승인**을 받는다(승인 없이 commit/push 금지).
5. 커밋 → `git push` 재시도(훅 재진입).
6. **예산**:
   - AutoFix(수정 라운드) ≤ 3, Total(push 재시도) ≤ 6.
   - 카운터: `<git-dir>/reviewloop-budget`(수정 라운드마다 +1, HEAD 브랜치 바뀌면 리셋).
   - 초과 시: `review-loop/logs/error_log.jsonl` 에 남기고 **종료**(사람에게 인계).

## 불변 규칙
- 훅·스크립트는 claude를 호출하지 않는다. 루프의 주체는 훅 바깥의 드라이버다.
- Minor는 push를 막지 않는다(러너는 Critical/미완성만 차단). 요청서는 "다음에 고칠 것" 안내다.
- Critical/미완성은 항상 사람. 드라이버가 임의로 고치지 않는다.
- 급할 때 우회: `git push --no-verify` (서버 CI(Gate 3)는 우회 불가).
