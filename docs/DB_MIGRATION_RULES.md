# Flyway Migration 운영 규칙

> 대상: 백엔드 4인 협업 프로젝트 (Java + MySQL + Flyway)
> 목적: 담당자별 버전 분리 + out-of-order 허용 + 운영 배포 안정성 확보

---

## 1. 기본 정책

현재 마이그레이션 상태:

```
V1__baseline.sql       ← 공통
V2__ai_scheduler.sql   ← 공통
V3__quiz.sql           ← 공통 (마지막 플랫 버전, 공유 DB에 이미 적용됨)
```

- `V1 ~ V3`은 **공통 마이그레이션**이며 이미 공유 DB에 적용(history에 기록)되어 있으므로 **이름·내용 모두 변경 금지.**
- 이후 모든 신규 DB 변경 사항은 **담당자별 영역(V3.1.x부터)** 으로 관리한다.
- Flyway는 `V3 = 3.0 < 3.1`로 비교하므로, 공통 V3 뒤에 담당자 버전이 순서대로 붙는다.
- **[참고] baseline**: 현재는 V1~V3이 정상 히스토리로 남아 있어 별도 squash/baseline 재설정이 불필요하다. 만약 향후 마이그레이션을 정리(squash)하게 되면, 운영/신규 환경 히스토리가 어긋나지 않도록 `baselineOnMigrate=true` + `baselineVersion`을 반드시 명시한다.

---

## 2. 담당자별 버전 영역

각 백엔드 담당자는 **배정받은 버전 영역만** 사용한다. (공통 V3 이후 신규 변경부터 적용)
Flyway는 동일 버전 번호를 허용하지 않으므로, 영역 분리는 파일명/버전 충돌을 원천 차단하는 협업 전략이다.

| 담당자 | 버전 영역 | 예시 |
| --- | --- | --- |
| 종호 | V3.1.x | `V3.1.1__add_membership_status_to_users.sql` |
| 종준 | V3.2.x | `V3.2.1__...sql` |
| 태연 | V3.3.x | `V3.3.1__...sql` |
| 강현 | V3.4.x | `V3.4.1__...sql` |

> 두 번째 자리(`.1` `.2` `.3` `.4`)가 **사람 구분자**, 세 번째 자리가 **그 사람의 순번**이다.
> 각자 자기 영역 안에서만 patch를 늘린다. (종호: `V3.1.1 → V3.1.2 → V3.1.3 ...`, 다음 사람 영역으로 넘어가지 않음)

---

## 3. Migration 파일 수정 금지

이미 GitHub에 Push 또는 Merge된 Migration 파일은 **수정하지 않는다.**

> **왜?** Flyway는 적용 시 파일의 checksum을 `flyway_schema_history`에 저장한다.
> 적용된 파일을 수정하면 저장된 checksum과 불일치가 발생해 `flyway validate`가 실패한다.
> 즉, 이건 팀 규칙이 아니라 Flyway의 물리적 제약이다.

- ❌ 금지: 이미 병합된 `V3.2.1__...sql` 내용 변경
- ✅ 허용: 새 파일 `V3.2.2__...sql`로 보완

---

## 4. Migration 파일 삭제 금지

생성·병합된 Migration 파일은 **삭제하지 않는다.**

- ❌ 금지: 병합된 마이그레이션 파일 삭제
- ✅ 허용: 잘못된 변경은 새 마이그레이션으로 되돌리기(revert)

---

## 5. Flyway 설정 (Out Of Order 허용)

프로젝트는 Out Of Order Migration을 허용한다. (`flyway.outOfOrder=true`)

예시 실행 순서:

```
V3.1.1 → V3.2.1 → V3.1.2 (뒤늦게 병합됨) → V3.3.1
```

위와 같이 낮은 버전이 뒤늦게 들어와도 정상 적용된다.

> **개념 정리 (오해 주의)**
> out-of-order를 켜도 **각 마이그레이션은 DB마다 딱 한 번만** 실행된다.
> 낮은 버전이 뒤늦게 들어오면 순서에 끼워넣어 1회 적용될 뿐, 같은 파일이 두 번 실행되지 않는다.

---

## 6. DB 변경은 반드시 Migration으로 관리

모든 스키마 변경은 Migration 파일로 관리한다.

- ❌ 금지: DB에 직접 SQL 실행 후 종료
- ✅ 허용: 파일 생성 후 Flyway로 적용

---

## 7. 공용 테이블 변경 시 사전 공유

`users`, `lectures`, `enrollments`, `payments` 등 공용 테이블 변경 시 반드시 팀 채널에 공유한다.
(out-of-order 환경에서 담당자 간 의존성 충돌을 막는 유일한 방어선이다.)

공유 양식:

```
[DB 변경 예정]
파일명: V3.3.2__add_membership_status_to_users.sql
대상: users
변경 내용: membership_status 컬럼 추가
영향 범위: 회원 / 결제 / 구독
```

---

## 8. Out-of-order 대응 작성 원칙 (중요)

로컬에서 사람이 DB를 수동으로 건드리는 상황이 섞일 수 있으므로, Migration은 **방어적으로** 작성한다.

- 권장: `IF NOT EXISTS` / `IF EXISTS` 사용

```sql
-- 권장
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS membership_status VARCHAR(20) DEFAULT 'FREE';
```

```sql
-- 지양 (이미 컬럼이 존재하면 실패)
ALTER TABLE users
  ADD COLUMN membership_status VARCHAR(20) DEFAULT 'FREE';
```

> **단, 이건 안전장치일 뿐이다.**
> `IF NOT EXISTS`는 out-of-order 때문에 필요한 게 아니라, **수동 DB 변경에 대비한 방어 코드**다.
> 남용하면 "왜 이 컬럼이 이미 있지?" 같은 진짜 문제를 덮어버릴 수 있으므로, 수동 DB 변경 자체를 지양한다.

### 8-1. MySQL DDL 롤백 불가 (필독)

MySQL은 `CREATE / ALTER / DROP` 같은 DDL이 **암묵적 커밋(implicit commit)** 을 일으킨다.
→ 한 파일 안에 DDL을 여러 개 넣으면, 중간에 실패해도 앞부분이 롤백되지 않아 **반쪽만 적용된 상태**로 남는다.

- **하나의 Migration 파일에는 논리적으로 하나의 변경만 담는다.**
- 여러 테이블/여러 DDL 변경은 파일을 나눈다.
- 이 원칙이 9번(로컬 DB 꼬임)의 발생 빈도 자체를 줄여준다.

---

## 9. 로컬 DB 오류 발생 시

Migration 충돌 또는 DB 상태가 꼬인 경우 아래 순서로 진행한다.

1. 로컬 DB 삭제
2. Flyway 초기화
3. 애플리케이션 재실행
4. V1 → V2 → V3.x 순서로 재적용

`flyway_schema_history` 테이블 직접 수정은 **금지**한다.

---

## 10. Migration 작성 규칙

파일명 형식:

```
V{버전}__{설명}.sql
예) V3.3.2__add_membership_status_to_users.sql
```

- 설명은 영문 snake_case로 작성한다.
- 버전은 배정받은 담당자 영역을 지킨다.
- 뷰/프로시저/시드 데이터처럼 반복 실행이 필요한 대상은 `R__` (Repeatable) 마이그레이션 사용을 검토한다.

---

## 11. 운영 환경 원칙

- 운영 DB에서는 기존 Migration 수정 금지
- 문제가 발생하면 새로운 Migration으로 보완
- 긴급 수정도 반드시 Migration 파일로 남긴다
- 운영 반영 전 CI 또는 스테이징에서 `flyway validate` 및 `flyway migrate` 확인
- **머지 순서 = 배포 순서**를 강제한다. out-of-order는 개발 편의를 위한 것이며, 운영에서는 공용 테이블 순서 의존성이 깨지지 않도록 병합/배포 순서 정합성을 유지한다.

---

## 최종 원칙 (반드시 지킬 것)

- [ ] Migration 파일 수정 금지 (checksum 불일치 → validate 실패)
- [ ] Migration 파일 삭제 금지
- [ ] DB 변경은 반드시 Migration으로 관리
- [ ] 담당 버전 영역만 사용
- [ ] 공용 테이블 변경 시 사전 공유
- [ ] out-of-order 환경 대비 방어적 SQL 작성 (`IF NOT EXISTS`, 단 남용 금지)
- [ ] 1파일 1논리변경 (MySQL DDL 롤백 불가)
- [ ] 기존 정리 시 baseline 처리 명시
- [ ] 충돌 시 로컬 DB 재생성 우선
- [ ] 운영 반영 전 validate + migrate 검증
- [ ] 머지 순서 = 배포 순서 유지
