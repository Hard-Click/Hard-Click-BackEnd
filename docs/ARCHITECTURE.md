# 아키텍처 의도 (ADR) & 이벤트 흐름

> 레이어 구조·의존 규칙은 코드와 `DEV_RULES.md`의 아키텍처 체크리스트에 있음.
> 여기엔 "코드만 봐선 실수인지 의도인지 모르는 것"만 기록.

## 의도적 결정 (되돌리지 마)
- **비정규화 count는 일부러다**: `posts.comment_count` 등은 조회 성능을 위해 의도적으로
  비정규화. "정규화 안 됐다"고 리팩터하지 말 것. 댓글 생성/삭제 시 count 동기화가 규칙.
- **타 도메인 참조는 Port + ReferenceEntity 패턴**: 다른 도메인 `JpaEntity` 직접 참조 금지.
  결합도 낮추려는 의도적 제약. (DEV_RULES 아키텍처 체크리스트 참조)
  - ReferenceEntity 작성 시 V1 baseline이 아닌 **최신 마이그레이션 기준** 테이블명 사용.
- **`id_sequences` 테이블**: 일부 도메인이 애플리케이션 레벨 ID 발번 사용 `<TODO: 의도/적용 범위 확인>`

## 이벤트 부수효과 흐름
- 도메인 이벤트로 부수효과 처리(알림 생성 등). **동기 이벤트**라 리스너에서 무거운 작업 주의.
- 관련: community / notice / report → notification 생성 흐름 `<TODO: 실제 이벤트 경로 확인>`

## 스키마 관리
- Flyway로만 관리, `ddl-auto: validate`. 스키마 변경은 `db/migration/V___.sql` 추가로만.
- 모든 PR은 **스키마 드리프트 CI 게이트**(임시 MySQL에 V1+V2 적용 후 validate 부팅) 통과 필수.

## 새 도메인 만들 때
- `cart` 도메인 구조를 복사 기준으로. 상세 체크리스트는 `DEV_RULES.md`(아키텍처 체크리스트) 참조.
