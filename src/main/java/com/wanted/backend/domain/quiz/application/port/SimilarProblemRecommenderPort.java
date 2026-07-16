package com.wanted.backend.domain.quiz.application.port;

import java.util.List;

/**
 * 오답 문항 기반 유사문제 추천 아웃바운드 포트.
 * 구현체(인프라)는 AI(Python/FastAPI) 추천 서버를 호출한다. problemId = quiz_question.id.
 * 반환: [원문제, 유사...] 형태(추천기 계약, result[0]=원문제) 또는 빈 리스트(추천 불가/서버 off).
 */
public interface SimilarProblemRecommenderPort {

    List<Long> recommendSimilar(long studentId, long problemId, int k);
}
