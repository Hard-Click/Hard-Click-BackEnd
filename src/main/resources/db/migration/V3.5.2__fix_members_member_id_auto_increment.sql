-- V1 baseline 작성 시 members.member_id에 AUTO_INCREMENT가 누락됨.
-- 엔티티는 GenerationType.IDENTITY라 INSERT 시 DB의 자동증가를 기대하는데,
-- 컬럼에 AUTO_INCREMENT가 없으면 "Field 'member_id' doesn't have a default value"로 실패한다.
-- 기존 운영 DB는 Flyway 도입 전 ddl-auto:update로 생성되어 이미 AUTO_INCREMENT가 있으므로 이 변경은 멱등하다.
ALTER TABLE members MODIFY member_id bigint NOT NULL AUTO_INCREMENT;
