package com.wanted.backend.domain.quiz.application.port;

import java.util.List;

/**
 * 복습 추천 아웃바운드 포트 — '무엇을 복습할지'(원문제)까지 AI 추천기가 고르는 정책('안 B').
 *
 * {@link SimilarProblemRecommenderPort}(원문제를 호출자가 주고 유사만 받음)와 달리,
 * 학생 id만으로 추천기가 이력에서 원문제를 선정하고 각 원문제의 유사문제까지 붙여 돌려준다.
 * 구현체(인프라)는 AI(Python/FastAPI) 서버의 {@code GET /quiz/review/{studentId}} 를 호출한다.
 * 반환: 급한 순 복습 항목 리스트, 또는 빈 리스트(추천 불가/서버 off).
 */
public interface ReviewRecommenderPort {

    List<ReviewItem> recommendReview(long studentId, int k);

    /**
     * 복습 항목 하나 = 원문제 + 그 원문제의 유사문제 id들.
     * problemId = quiz_question.id(원문제), sectionId = 원문제가 속한 섹션,
     * courseId = 원문제(=유사문제)가 속한 코스(그룹별 SimilarQuiz 저장에 사용, 미상이면 0),
     * similarIds = 유사문제 문항 id.
     */
    record ReviewItem(long problemId, long sectionId, long courseId, List<Long> similarIds) {}
}
