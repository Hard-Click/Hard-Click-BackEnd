-- V3.5.2(members)와 동일한 결함: V1 baseline 작성 시 AUTO_INCREMENT 누락.
-- 엔티티는 GenerationType.IDENTITY라 INSERT 시 DB 자동증가를 기대하는데, 컬럼에 AUTO_INCREMENT가
-- 없으면 "Field '...' doesn't have a default value"로 실패한다.
--   - subscriptions : 구독 결제 후 구독 미생성(과금됐는데 미지급)의 직접 원인.
--   - video_progress: 동일 결함(전수 점검 중 추가 발견) — 신규 스키마에서 첫 영상 진도 저장 실패.
-- 기존 운영 DB는 Flyway 도입 전 ddl-auto:update로 생성되어 이미 AUTO_INCREMENT가 있으므로 이 변경은 멱등하다.
--
-- 전수 점검 결과 아래는 의도적으로 제외(고치면 오히려 깨짐):
--   - orders / order_items      : id_sequences 기반 TABLE 제너레이터
--   - subscription_plans        : 앱 지정(카탈로그 시드) PK
--   - id_sequences              : 시퀀스 테이블 자체(varchar PK)
--   - course_curriculum / video : 활성 엔티티 매핑 없는 레거시 테이블

ALTER TABLE subscriptions  MODIFY subscription_id bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE video_progress MODIFY progress_id     bigint NOT NULL AUTO_INCREMENT;
