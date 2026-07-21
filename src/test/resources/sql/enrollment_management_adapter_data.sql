DELETE FROM video_progress;
DELETE FROM enrollment;
DELETE FROM lesson;
DELETE FROM course_section;
DELETE FROM course;

INSERT INTO course (course_id, title, thumbnail_url, status) VALUES (20, 'Spring Course', null, 'PUBLISHED');
-- 소프트 삭제된 강의: 수강 중이어도 목록에서 제외돼야 한다.
INSERT INTO course (course_id, title, thumbnail_url, status) VALUES (21, 'Deleted Course', null, 'DELETED');

-- 섹션 2개를 의도적으로 order_index와 id 순서가 어긋나게 둬서, id가 아니라 order_index가
-- 정렬을 결정하는지(섹션 경계를 넘는 정렬 보존까지) 검증한다.
INSERT INTO course_section (id, course_id, order_index) VALUES (50, 20, 1);
INSERT INTO course_section (id, course_id, order_index) VALUES (31, 20, 0);

-- section 31(order_index 0) 안에서도 id와 order_index 순서를 어긋나게 둔다.
INSERT INTO lesson (id, section_id, order_index) VALUES (10, 31, 1);
INSERT INTO lesson (id, section_id, order_index) VALUES (15, 31, 0);
INSERT INTO lesson (id, section_id, order_index) VALUES (70, 50, 0);

-- member 1: 진도 있음 -> lastVideoId는 lastProgress 경로
INSERT INTO video_progress (
    progress_id, member_id, course_id, video_id, last_position_sec, is_completed, updated_at
) VALUES (
    100, 1, 20, 10, 42, 1, CURRENT_TIMESTAMP
);

INSERT INTO enrollment (
    enrollment_id, member_id, course_id, enrolled_at, status, expired_at
) VALUES (
    200, 1, 20, CURRENT_TIMESTAMP, 'IN_PROGRESS', null
);

-- member 2: 진도 없음 -> lastVideoId는 firstVideoId(firstLesson) 경로 (정렬 1순위 레슨)
INSERT INTO enrollment (
    enrollment_id, member_id, course_id, enrolled_at, status, expired_at
) VALUES (
    201, 2, 20, CURRENT_TIMESTAMP, 'IN_PROGRESS', null
);

-- member 3: 마이코스 목록 대상이 아닌 상태(REFUNDED) -> 결과에서 완전히 제외돼야 함
INSERT INTO enrollment (
    enrollment_id, member_id, course_id, enrolled_at, status, expired_at
) VALUES (
    202, 3, 20, CURRENT_TIMESTAMP, 'REFUNDED', null
);

-- member 4: 수강 상태는 정상(IN_PROGRESS)이지만 강의가 소프트 삭제(status=DELETED) -> 목록에서 제외돼야 함
INSERT INTO enrollment (
    enrollment_id, member_id, course_id, enrolled_at, status, expired_at
) VALUES (
    203, 4, 21, CURRENT_TIMESTAMP, 'IN_PROGRESS', null
);
