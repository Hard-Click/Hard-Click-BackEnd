package com.wanted.backend.domain.quiz.application.result;

import java.util.List;

/**
 * 복습 추천 결과(application) — 원문제별로 유사문제를 묶어 급한 순으로 담는다.
 * 학생 전체 이력 기반(코스 경계 없음)이라 {@link SimilarQuizResult}와 달리 courseId/week가 없다.
 * 정답/해설은 응시용 응답에 포함하지 않는다. (영속·제출 경로는 후속 — courseId 정책 확정 필요)
 */
public record ReviewQuizResult(List<ReviewGroup> reviews) {

    /** 원문제 하나 + 그에 붙는 유사문제(응시 대상). */
    public record ReviewGroup(Long originalQuestionId, Long sectionId, List<Question> similar) {}

    public record Question(Long questionId, String content, List<String> options) {}
}
