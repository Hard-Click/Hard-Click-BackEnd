-- 스터디 모집(study) + 채팅방(chat_room). study/chat은 서로 다른 최상위 도메인이라
-- chat_room.study_id는 FK 없이 컬럼만 둔다 (quiz.course_id와 동일한 Port 패턴, ARCHITECTURE.md 참조).
-- host_id/member_id는 identity 도메인 참조라 마찬가지로 FK 없음.

CREATE TABLE study (
    study_id      BIGINT       NOT NULL AUTO_INCREMENT,
    host_id       BIGINT       NOT NULL,
    title         VARCHAR(300) NOT NULL,
    subject       VARCHAR(50)  NOT NULL,
    content       TEXT         NOT NULL,
    max_count     INT          NOT NULL,
    current_count INT          NOT NULL DEFAULT 1,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (study_id),
    KEY idx_study_status_created (status, created_at),
    KEY idx_study_host_id (host_id),
    CHECK (max_count >= 2)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE study_participant (
    study_participant_id BIGINT   NOT NULL AUTO_INCREMENT,
    study_id             BIGINT   NOT NULL,
    member_id            BIGINT   NOT NULL,
    joined_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (study_participant_id),
    UNIQUE KEY uk_study_participant_study_member (study_id, member_id),
    CONSTRAINT fk_study_participant_study FOREIGN KEY (study_id) REFERENCES study (study_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_room (
    chat_room_id BIGINT      NOT NULL AUTO_INCREMENT,
    study_id     BIGINT      NOT NULL,
    host_id      BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_room_id),
    UNIQUE KEY uk_chat_room_study_id (study_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_room_participant (
    chat_room_participant_id BIGINT   NOT NULL AUTO_INCREMENT,
    chat_room_id             BIGINT   NOT NULL,
    member_id                BIGINT   NOT NULL,
    joined_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_room_participant_id),
    UNIQUE KEY uk_chat_room_participant_room_member (chat_room_id, member_id),
    CONSTRAINT fk_chat_room_participant_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (chat_room_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
