# 도메인 명세 고정값 (⚠️ 임의 변경 금지)

코드에는 "결과 숫자"만 있고 근거(기획 명세)는 없다. AI/개발자가 이 값을 마음대로
바꾸는 사고를 막기 위한 문서. 값 변경은 반드시 기획 확인 후.

> `<TODO>` = 코드/기획에서 정확한 값 확인 필요 (팀이 채울 것)

## 인증 / 계정 (identity)
- 이메일: **gmail.com 도메인만 허용** (`identity.email.allowed-domain`)
- 인증코드 TTL: **5분** (300000ms) · 인증완료 토큰 TTL 5분
- 비밀번호 재발급: **하루 3회** 제한 (`password-reset-daily-limit`)
- 로그인 **5회 실패 시 계정 잠금** (`members.login_fail_count` / `is_locked`) — `Member.MAX_LOGIN_FAIL_COUNT`, 잠금 시 `U004 ACCOUNT_LOCKED`
- 비밀번호 변경 시 **기존 비밀번호 재사용 금지** — 새 비밀번호가 기존과 동일하면 `AUTH_012 PASSWORD_REUSE_NOT_ALLOWED`(400). 적용 경로: 일반 변경 / 이메일 재설정 / 계정잠금 해제 (3개 전부)
- JWT: access **30분**(1800000ms) / refresh **14일**(1209600000ms)

## 파일 업로드
- 프로필/게시글/댓글 이미지: **5MB** (5242880 bytes)
- ⚠️ `application.yaml` multipart 한도는 **5GB** — 위 도메인 제한(5MB)과 다름, 혼동 주의
- 영상 파일 한도: **1GB** (초과 시 `F005 VIDEO_FILE_SIZE_EXCEEDED`) — multipart 5GB 한도와 별개인 도메인 검증값

## 구독 (subscription)
- 연간 패스 가격 = **(다가오는 수능일까지 남은 일수) × 30,000원** (`daily-rate`)
- suneung-date: **2026-11-19** — 매년 갱신 필요 (미갱신 시 다음 해 같은 날 자동 롤오버)

## 순공 타이머 (study_timer)
- 회원 행 락 타임아웃: **3초** (3000ms) — 초과 시 `STUDY_TIMER_LOCK_TIMEOUT`

## Redis 캐시 키 (개발규칙 기준)
- `grass:{memberId}:{yyyy-MM}` (TTL 자정까지)
- `stats:daily:{memberId}:{yyyy-MM-dd}` (24h)
- `ranking:{period}:{type}:{page}` (60min)
