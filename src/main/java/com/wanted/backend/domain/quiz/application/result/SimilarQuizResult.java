package com.wanted.backend.domain.quiz.application.result;

import java.util.List;

/**
 * 유사퀴즈 생성 결과(application). 생성(①) 시 영속되며 similarQuizId는 실제 식별자다(제출 ②에서 사용).
 * 정답/해설은 응시용 응답에 포함하지 않는다.
 */
public record SimilarQuizResult(
        Long similarQuizId,
        Long courseId,
        Integer week,
        String title,
        List<Question> questions
) {
    public record Question(Long questionId, String content, List<String> options) {}
}
