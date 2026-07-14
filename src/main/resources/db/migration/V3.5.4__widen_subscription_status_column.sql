-- subscriptions.status가 V1 baseline에서 enum('ACTIVE') 단일값으로만 정의되어,
-- 구독 취소/만료 시 'CANCELLED'/'EXPIRED' 기록이 "Data truncated for column 'status'"로 실패한다.
-- (POST /api/order/{orderId}/refund → RefundSubscriptionService → CancelSubscriptionService 에서 발생)
--
-- V3.5.3 이전엔 subscription_id AUTO_INCREMENT 누락으로 구독 생성 자체가 불가했어서
-- 이 취소/환불 경로가 프로덕션에서 한 번도 실행된 적이 없었고, 그래서 잠복해 있던 결함이다.
--
-- 엔티티(SubscriptionJpaEntity.status)는 String(length=20) 매핑이므로 varchar(20)으로 맞춘다.
-- (enum 값 추가 방식 대신 varchar로 넓혀 향후 상태값 추가 시 재발을 방지하고 매핑과 일치시킨다.)
-- 기존 'ACTIVE' 값은 문자열로 그대로 보존된다.

ALTER TABLE subscriptions MODIFY status VARCHAR(20) NOT NULL;
