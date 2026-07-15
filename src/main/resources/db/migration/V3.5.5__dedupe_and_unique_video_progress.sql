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
ALTER TABLE video_progress
    ADD CONSTRAINT uk_video_progress_member_video UNIQUE (member_id, video_id);
