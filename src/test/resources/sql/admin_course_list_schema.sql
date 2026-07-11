-- 관리자 강의 목록 어댑터 테스트용 최소 스키마 (참조 엔티티가 읽는 컬럼만).
-- 전체 엔티티 DDL 생성/두-엔티티 충돌을 피하려 ddl-auto=none + 이 스키마를 사용한다.

DROP TABLE IF EXISTS course;
CREATE TABLE course (
    course_id  BIGINT       NOT NULL PRIMARY KEY,
    title      VARCHAR(255),
    author_id  BIGINT,
    subject    VARCHAR(255),
    status     VARCHAR(20),
    created_at TIMESTAMP
);

DROP TABLE IF EXISTS enrollment;
CREATE TABLE enrollment (
    enrollment_id BIGINT      NOT NULL PRIMARY KEY,
    member_id     BIGINT      NOT NULL,
    course_id     BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL,
    expired_at    TIMESTAMP
);
