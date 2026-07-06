# 백엔드 개발 규칙

> 원본 "백엔드 개발규칙 v4"(2026-06-15) 기준. **2026-07-06 RDS 이전 반영** (아래 ⟳ 표시 3곳).

---

## 🔴 즉시 적용 — 절대 규칙

### ddl-auto: update 사용 금지
스키마 변경은 Flyway 마이그레이션 파일로만 처리. **해당 설정이 들어간 커밋은 리뷰 반려.**

### ⟳ 스키마 드리프트 CI 게이트 (2026-07-06 신규)
- 모든 PR은 임시 MySQL에 Flyway(V1+V2) 적용 후 `ddl-auto: validate` 부팅 검증을 통과해야 머지 가능.
- 엔티티↔스키마 불일치(드리프트)가 있으면 CI 빨강 → 머지 차단.
- 스키마 변경은 반드시 `src/main/resources/db/migration/V___.sql` 로만.

---

## 브랜치 전략
```
main ← develop ← feature/{이슈번호}-{기능명}
```
- `develop` 직접 push 금지, PR 필수
- `GANGHYEON02` approve 없으면 merge 불가
- 마이그레이션 파일은 `Yoonjongho1122` approve 추가
- `main` merge: `GANGHYEON02` + `jongjun` 둘 다 approve 필요

## 커밋 컨벤션
```
feat / fix / refactor / style / chore / docs / test
```
PR 제목 형식: `[feat] 결제 도메인 skeleton`

## PR 작성 규칙
PR 본문 최소 항목 + 체크리스트:
- [ ] 로컬 빌드 통과 (`./gradlew build`)
- [ ] Flyway 파일 포함 여부 확인
- [ ] ddl-auto: update 없음
- [ ] MinIO 설정 없음
- [ ] InfluxDB 설정 없음

### 아키텍처 체크리스트 (신규 도메인 PR 필수)
- [ ] `domain/model`에 Spring·JPA import 없음
- [ ] `application`이 `infrastructure` 직접 참조 없음
- [ ] 타 도메인 `JpaEntity` 직접 참조 없음 (Port + ReferenceEntity 패턴)
- [ ] `create()` / `restore()` 팩토리 메서드 존재
- [ ] Flyway 마이그레이션 파일 포함
- [ ] Swagger `@Operation` 어노테이션 작성

## CI/CD 워크플로우 (6단계)
1. 기능 개발 및 push → `push-ci.yml` 자동 실행
2. PR 생성 (base: develop) — Draft면 CI·CodeRabbit 안 돎
3. 자동 실행: `pr-ci.yml`(빌드·테스트·스키마게이트) + CodeRabbit 리뷰
4. 리뷰 대응 → `GANGHYEON02` Reviewer 지정
5. 리더 승인 (마이그레이션 포함 시 `Yoonjongho1122` 추가)
6. Merge → 로컬 브랜치 정리

### CodeRabbit 명령어
`@coderabbitai review` / `full review` / `summary`

## CD 파이프라인
- `cd.yml` — `main` push 시 자동 빌드 → Docker Hub → EC2 배포
- Dockerfile — `gradlew` 실행권한(`chmod +x`) 필수
- ⟳ **`docker-compose.prod.yml` — Redis + App 컨테이너 (MySQL은 RDS로 분리, 로컬 mysql 컨테이너 제거)** *(구: MySQL+Redis+App 3개)*

## GitHub Secrets 목록
DB_URL · DB_USERNAME · DB_PASSWORD · JWT_SECRET · MAIL_USERNAME · MAIL_PASSWORD · REDIS_PASSWORD · AWS_ACCESS_KEY_ID · AWS_SECRET_ACCESS_KEY · S3_BUCKET · S3_PRESIGN_ACCESS_KEY · S3_PRESIGN_SECRET_KEY · DOCKER_USERNAME · DOCKER_PASSWORD · EC2_HOST · EC2_USERNAME · EC2_SSH_KEY · SENTRY_DSN

## Flyway + H2 테스트 환경
```groovy
tasks.withType(Test) { systemProperty 'spring.flyway.enabled', 'false' }
```

## 인프라 확정 스펙
| 항목 | 값 |
|------|-----|
| 서버 | EC2 t3.small 1대 (서울) |
| 레지스트리 | Docker Hub |
| CI/CD | GitHub Actions |
| 빌드 | `--platform linux/amd64` 강제 |
| ⟳ **DB** | **AWS RDS (MySQL 8) — 앱 EC2에서 분리, 자동 백업** *(구: MySQL 8 EC2 내 DB 2개 분리)* |
| 캐시 | Redis 7-alpine (maxmemory 128mb, 비밀번호 인증) |
| 파일 저장 | AWS S3 (버킷: hard-click-bucket-970636746023-ap-northeast-2-an) |
| 모니터링 | Prometheus + Grafana + Sentry |

## 알려진 이슈
- enrollment 테스트 컴파일 에러: `MyEnrolledCourseData` record 9필드인데 테스트 8인자 → `enrollmentStatus` 인자 추가 필요
