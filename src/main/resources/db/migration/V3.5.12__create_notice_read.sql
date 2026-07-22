-- 공지 읽음 상태를 1급 개념으로 분리.
-- 기존에는 notification 행(redirect_url='/notices/{id}' + is_read)에서 파생했으나,
-- 알림 행이 없는 회원(공지 이후 가입 등)은 읽음 추적이 불가능했다.
-- 이 테이블로 알림 존재 여부와 무관하게 (회원, 공지) 단위 읽음을 정확히 관리한다.
CREATE TABLE notice_read (
    id        BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    notice_id BIGINT      NOT NULL,
    -- Hibernate 6는 LocalDateTime을 datetime(6)으로 매핑한다. ddl-auto=validate 드리프트 방지를 위해 정밀도 일치.
    read_at   datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- uk_notice_read_member_notice가 (member_id, notice_id) 복합 인덱스라 member_id 선행 조회를
    -- 이미 커버한다. 별도 member_id 단독 인덱스는 중복이므로 두지 않는다.
    UNIQUE KEY uk_notice_read_member_notice (member_id, notice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 기존 알림 기반 읽음 상태 백필: redirect_url='/notices/{숫자}' 이고 is_read=true 인 알림.
-- read_at은 실제 읽은 시각이 없으므로 알림 생성시각(created_at)의 최솟값으로 근사한다.
-- 회원·공지별 중복 알림이 있을 수 있어 GROUP BY로 1행으로 합친다.
INSERT INTO notice_read (member_id, notice_id, read_at)
SELECT n.receiver_id,
       CAST(SUBSTRING(n.redirect_url, CHAR_LENGTH('/notices/') + 1) AS UNSIGNED) AS notice_id,
       MIN(n.created_at) AS read_at
FROM notification n
WHERE n.redirect_url LIKE '/notices/%'
  AND n.is_read = TRUE
  AND SUBSTRING(n.redirect_url, CHAR_LENGTH('/notices/') + 1) REGEXP '^[0-9]+$'
GROUP BY n.receiver_id,
         CAST(SUBSTRING(n.redirect_url, CHAR_LENGTH('/notices/') + 1) AS UNSIGNED);
