-- 강의: PUBLISHED/DRAFT는 노출, DELETED는 제외. 과목/강사/검색어/정렬 검증용.
INSERT INTO course (course_id, title, author_id, subject, status, created_at) VALUES
  (1, 'React 완벽 가이드', 10, '프로그래밍', 'PUBLISHED', TIMESTAMP '2026-05-10 00:00:00'),
  (2, 'React 심화',       10, '프로그래밍', 'DRAFT',     TIMESTAMP '2026-05-12 00:00:00'),
  (3, '삭제된 강의',       10, '프로그래밍', 'DELETED',   TIMESTAMP '2026-06-01 00:00:00'),
  (4, '수1 정복하기',      20, '수학1',      'PUBLISHED', TIMESTAMP '2026-04-22 00:00:00');

-- course 1 수강생: 활성(IN_PROGRESS/COMPLETED, 미만료) 고유 member = {100, 101, 102} → 3명.
--  - member 100: 만료 null 활성
--  - member 101: 미래 만료 활성
--  - member 102: 활성 2건(중복) → dedup되어 1명으로 카운트
--  - member 103: EXPIRED 상태 → 제외
--  - member 104: 과거 만료 → 제외
INSERT INTO enrollment (enrollment_id, member_id, course_id, status, expired_at) VALUES
  (1, 100, 1, 'IN_PROGRESS', NULL),
  (2, 101, 1, 'COMPLETED',   TIMESTAMP '2099-01-01 00:00:00'),
  (3, 102, 1, 'IN_PROGRESS', NULL),
  (4, 102, 1, 'COMPLETED',   TIMESTAMP '2099-01-01 00:00:00'),
  (5, 103, 1, 'EXPIRED',     NULL),
  (6, 104, 1, 'IN_PROGRESS', TIMESTAMP '2020-01-01 00:00:00'),
  (7, 200, 4, 'IN_PROGRESS', NULL);
