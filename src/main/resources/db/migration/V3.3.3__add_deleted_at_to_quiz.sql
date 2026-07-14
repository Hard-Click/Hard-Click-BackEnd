-- 섹션 삭제(강의 수정) 시 퀴즈를 hard-delete하면 학생 제출 이력(quiz_submission)까지
-- FK ON DELETE CASCADE로 함께 사라진다. 이를 막기 위해 퀴즈를 soft-delete(deleted_at)로 전환한다.
--
-- deleted_at IS NULL = 활성 퀴즈. 목록/상세/제출/수정 등 활성 조회는 이 조건으로 필터한다.
-- (학생 과거 리포트는 삭제된 퀴즈도 조회 가능해야 하므로 별도 경로에서 필터 없이 로딩한다.)
-- 기존 행은 NULL(활성)로 채워진다.

ALTER TABLE quiz
    ADD COLUMN deleted_at DATETIME NULL COMMENT '소프트 삭제 시각 (NULL=활성)';
