-- 관리자 결제 목록(orders 기반) 어댑터 테스트용 최소 스키마 (참조 엔티티가 읽는 컬럼만).
-- 전체 엔티티 DDL 생성/두-엔티티(orders를 매핑하는 읽기/쓰기 엔티티) 충돌을 피하려 ddl-auto=none + 이 스키마를 사용한다.

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    order_id     BIGINT       NOT NULL PRIMARY KEY,
    member_id    BIGINT       NOT NULL,
    order_no     VARCHAR(255) NOT NULL,
    payment_type VARCHAR(30)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    final_amount INT          NOT NULL,
    paid_at      TIMESTAMP
);

DROP TABLE IF EXISTS members;
CREATE TABLE members (
    member_id BIGINT       NOT NULL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL
);
