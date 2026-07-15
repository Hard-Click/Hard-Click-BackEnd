-- video_progress에 (member_id, video_id) 유니크 제약이 없어 첫 시청 시 동시 요청이
-- 각자 INSERT를 수행, 중복 행이 쌓였다. 중복이 생기면 findByMemberIdAndVideoId(Optional)가
-- NonUniqueResultException을 던져 해당 회원+영상의 재생/진도저장이 영구적으로 실패한다.

-- 1) 중복 행 정리: 조합당 가장 많이 진행된 행 하나만 남긴다.
--    (완료 > 시청시간 > 재생위치 > 최신 순으로 우선순위를 두어 진도 손실을 막는다)
DELETE FROM video_progress
WHERE progress_id IN (
    SELECT progress_id
    FROM (
        SELECT progress_id,
               ROW_NUMBER() OVER (
                   PARTITION BY member_id, video_id
                   ORDER BY is_completed      DESC,
                            watch_time_sec    DESC,
                            last_position_sec DESC,
                            progress_id       DESC
               ) AS rn
        FROM video_progress
    ) ranked
    WHERE ranked.rn > 1
);

-- 2) 재발 방지: DB 레벨에서 중복을 차단한다.
--    video_progress는 재생 중 계속 쓰기가 들어오는 테이블이라, 테이블 재빌드(COPY)로 떨어지면
--    배포 중 쓰기가 오래 막힌다. INPLACE/LOCK=NONE을 명시해 그 경우 조용히 COPY로 가는 대신
--    마이그레이션이 실패하도록 한다.
ALTER TABLE video_progress
    ADD CONSTRAINT uk_video_progress_member_video UNIQUE (member_id, video_id),
    ALGORITHM = INPLACE, LOCK = NONE;
