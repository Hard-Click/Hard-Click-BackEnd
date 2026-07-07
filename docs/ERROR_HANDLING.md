# 에러 처리 규약

## 응답 형태
- 성공: `ApiResponse.success(...)` / `created(...)`
- 실패: `throw new BusinessException(ErrorCode.XXX)` → `GlobalExceptionHandler`가 변환
- 에러 JSON: `{ errorCode, message, path, details }` (`ErrorResponse`)

## 방어선 (GlobalExceptionHandler)
1. `@Valid` / 파라미터 / 타입 / 본문 파싱 → 400 (WARN)
2. 도메인 검증 예외 → 400 (WARN)
3. `BusinessException` → ErrorCode의 status (WARN) — **Sentry 미전송**(예상된 에러)
4. `AccessDenied` → 403
5. 그 외 모든 예외 → 500 (ERROR) + **Sentry 전송**(domain/path/method/exceptionType 태그 부착)

## ErrorCode 접두어 표 (실제 코드 기준)
| 접두어 | 도메인 | 개수 |
|---|---|---|
| U | 유저/인증 | 20 |
| ST | 순공 타이머(study_timer) | 27 |
| C | 댓글(comment) | 15 |
| P | 게시글(post) | 10 |
| ORD | 주문(order) | 10 |
| F | 파일 업로드 | 6 |
| RP | 신고(report) | 5 |
| N | 공지(notice) | 5 |
| L | 강의/학습(lesson) | 5 |
| CR | 강의(course) | 4 |
| R | 리뷰(review) | 4 |
| DS | 일일 학습통계 | 4 |
| NT | 알림(notification) | 2 |
| CART / WISH / SUB | 장바구니 / 위시리스트 / 구독 | 각 2 |
| CO / EN | 커뮤니티(community) / 수강(enrollment) | 각 1 |

## 새 ErrorCode 추가 규칙
- 도메인 접두어 + 3자리 번호 (예: `ORD011`)
- ⚠️ 접두어 신규 지정 시 위 표와 **충돌 금지** (특히 한 글자 C·P·N·R·L·F 계열)
