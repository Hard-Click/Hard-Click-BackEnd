-- 퀴즈 답안에 문제별 풀이 시간(time_spent_seconds) 추가.
-- 프론트가 문제 화면 체류 시간을 측정해 제출 시 답안마다 함께 보낸다(재방문 시 누적).
-- 복습 추천 개인화에서 "맞았지만 오래 고민한 문제" / "너무 빨리 찍은 문제" 판별에 사용한다.
-- 기존 행과 시간 미전송 클라이언트는 NULL(미측정) — 추천 코드는 NULL이면 시간 신호를 건너뛴다.

ALTER TABLE quiz_submission_answer
    ADD COLUMN time_spent_seconds INT NULL COMMENT '문제 풀이 체류 시간(초, 재방문 누적). NULL=미측정';
