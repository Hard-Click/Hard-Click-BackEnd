-- 퀴즈 문항에 난이도(difficulty) 추가.
-- 유사문제 추천 서비스(quiz_recommender)의 난이도 기반 폴백에서 사용한다.
-- 정수 레벨: 1=하, 2=중, 3=상.
-- 기존 문항은 NULL(미지정) — 추천 코드는 NULL이면 난이도 폴백을 건너뛰고 section/course로만 검색한다.

ALTER TABLE quiz_question
    ADD COLUMN difficulty TINYINT NULL COMMENT '난이도 (1=하, 2=중, 3=상)';
