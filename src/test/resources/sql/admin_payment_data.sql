-- 회원 참조(members): 100/101만 존재. 999는 일부러 없음(삭제된 회원 참조 시나리오).
INSERT INTO members (member_id, name, email) VALUES
    (100, '김근지', 'geunji04@example.com'),
    (101, '이철수', 'chulsoo@example.com');

-- 실제 결제 확정은 orders에만 기록된다. payments 행은 없다(미사용 데모 경로).
-- 상태: PAID/PARTIAL_REFUNDED/REFUNDED는 노출, READY(미결제)는 제외 대상.
INSERT INTO orders (order_id, member_id, order_no, payment_type, status, final_amount, paid_at) VALUES
    (203, 100, 'ORD-20260720-90BABF11', 'SUBSCRIPTION', 'PAID',             3660000, '2026-07-20 10:00:00'),
    (204, 101, 'ORD-20260719-COURSE01', 'COURSE',       'PAID',               89000, '2026-07-19 09:00:00'),
    (205, 100, 'ORD-20260718-REFUND01', 'COURSE',       'REFUNDED',           89000, '2026-07-18 08:00:00'),
    (206, 101, 'ORD-20260721-READY001', 'COURSE',       'READY',              50000, NULL),
    (207, 999, 'ORD-20260717-DELMEM01', 'SUBSCRIPTION', 'PAID',             3660000, '2026-07-18 08:00:00'),
    (208, 100, 'ORD-20260716-PARTIAL1', 'COURSE',       'PARTIAL_REFUNDED',   89000, '2026-07-16 06:00:00');
