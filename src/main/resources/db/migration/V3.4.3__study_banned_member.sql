-- 스터디 강퇴 이력(재입장 차단용). study_participant와 동일 패턴.

CREATE TABLE study_banned_member (
    study_banned_member_id BIGINT   NOT NULL AUTO_INCREMENT,
    study_id                BIGINT   NOT NULL,
    member_id               BIGINT   NOT NULL,
    banned_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (study_banned_member_id),
    UNIQUE KEY uk_study_banned_member_study_member (study_id, member_id),
    CONSTRAINT fk_study_banned_member_study FOREIGN KEY (study_id) REFERENCES study (study_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
