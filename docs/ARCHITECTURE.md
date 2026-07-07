# 아키텍처 의도 (ADR) & 이벤트 흐름

> 레이어 구조·의존 규칙은 코드와 `DEV_RULES.md`의 아키텍처 체크리스트에 있음.
> 여기엔 "코드만 봐선 실수인지 의도인지 모르는 것"만 기록.

## 의도적 결정 (되돌리지 마)
- **비정규화 count는 일부러다**: `posts.comment_count` 등은 조회 성능을 위해 의도적으로
  비정규화. "정규화 안 됐다"고 리팩터하지 말 것. 댓글 생성/삭제 시 count 동기화가 규칙.
- **타 도메인 참조는 Port + ReferenceEntity 패턴**: 다른 도메인 `JpaEntity` 직접 참조 금지.
  결합도 낮추려는 의도적 제약. (DEV_RULES 아키텍처 체크리스트 참조)
  - ReferenceEntity 작성 시 V1 baseline이 아닌 **최신 마이그레이션 기준** 테이블명 사용.
- **`id_sequences` 테이블**: `order` 도메인의 `orders`/`order_items`만 사용. 기존 테이블에
  AUTO_INCREMENT가 없어 JPA `GenerationType.TABLE`(`@TableGenerator`, allocationSize=50)로
  앱 레벨 발번 — 스키마 변경 없이 호환 유지하려는 의도. 다른 도메인은 IDENTITY 사용.

## 이벤트 부수효과 흐름 (notification 생성)
- 도메인 서비스가 `ApplicationEventPublisher.publishEvent()`로 `DomainEvent`(record) 발행 →
  `notification` 도메인의 `NotificationEventListener`가 수신 → `NotificationCommandUseCase.send()/sendBatch()`
  → Notification 생성·저장 + SSE 실시간 발송.
- ⚠️ **비동기 + 커밋 후** 실행이다 (`@Async("notificationExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`).
  문서 이전 표기 "동기"는 **오류였음**. 원 트랜잭션 커밋 뒤 별도 스레드에서 돌기 때문에, 리스너가 실패해도
  본 로직은 롤백 안 됨 → **알림 유실 가능성**을 전제로 볼 것. 스레드풀(`AsyncConfig`): core 4 / max 10 / queue 200 (prefix `Notification-`).
- 발행 행위 → 이벤트 → 수신자 매핑:

  | 행위 | 이벤트 | 수신자 | NotificationType |
  |---|---|---|---|
  | 게시글 댓글 | `PostCommentCreatedEvent` | 글쓴이(본인이면 skip) | POST_COMMENT |
  | 대댓글 | `CommentReplyCreatedEvent` | 부모 댓글 작성자(본인이면 skip) | COMMENT_REPLY |
  | 댓글 채택 | `CommentAcceptedEvent` | 댓글 작성자 | COMMENT_ACCEPTED |
  | 신고 접수 | `ReportCreatedEvent` | 관리자 전원 | REPORT |
  | 강의 공지 | `NoticeCreatedEvent`(COURSE) | 수강생 (+강사 작성 시 관리자 전원) | NOTICE |
  | 전체 공지 | `NoticeCreatedEvent`(GLOBAL) | 강사·학생 전원 | NOTICE |
  | 강좌 개설 | `CourseCreatedEvent` | 관리자 전원 | COURSE_REGISTER |

  - 발행부: community(`CommentCommandService`·`ReportCommandService`), notice(`NoticeCommandService`), cource(`CourseCommandService`).
  - 수신부: `domain/notification/application/listener/NotificationEventListener` (리스너 6개).

## 스키마 관리
- Flyway로만 관리, `ddl-auto: validate`. 스키마 변경은 `db/migration/V___.sql` 추가로만.
- 모든 PR은 **스키마 드리프트 CI 게이트**(임시 MySQL에 V1+V2 적용 후 validate 부팅) 통과 필수.

## 새 도메인 만들 때
- `cart` 도메인 구조를 복사 기준으로. 상세 체크리스트는 `DEV_RULES.md`(아키텍처 체크리스트) 참조.
