package com.wanted.backend.domain.quiz.application.result;

import java.util.List;

/**
 * 유사퀴즈 생성 결과(application). 무상태(미영속) 프리뷰 — 제출/영속은 후속 작업.
 * similarQuizId는 현재 courseId placeholder(제출 엔드포인트 도입 시 실 식별자로 교체).
 */
public record SimilarQuizResult(
        Long similarQuizId,
        Long courseId,
        String title,
        List<Question> questions
) {
    public record Question(Long questionId, String content, List<String> options) {}
}
