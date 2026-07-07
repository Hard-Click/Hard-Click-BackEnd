# community 도메인 규칙

- **비정규화 count 유지**: `posts.comment_count`는 의도적 비정규화. 댓글 생성/삭제 시
  동기화하는 게 규칙. "실시간 COUNT 쿼리로 바꾸기" 금지(성능 결정).
- **캐시**: 게시글 카운트/목록 캐시 무효화 규칙 준수.
  - `postCount:v1` (key=`boardType`/`ALL`, **TTL 30초**, `RedisConfig`) — 게시글 건수
  - `reviews:guest` (key=`courseId:sort:page`, **TTL 10분**, 비회원 `memberId==-1`만 캐싱) — 리뷰 목록
- **신고 자동정지**: 신고 누적 임계값 도달 시 자동 정지 — 동시성 레이스 이미 처리됨,
  카운트 집계 방식(ScalarSubquery) 임의 변경 금지.
