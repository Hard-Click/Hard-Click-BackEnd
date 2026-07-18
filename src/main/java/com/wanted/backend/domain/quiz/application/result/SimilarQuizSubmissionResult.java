package com.wanted.backend.domain.quiz.application.result;

import java.util.List;

/**
 * 유사퀴즈 제출·채점 결과(application). 해설 화면을 바로 렌더할 수 있게 정답·내답·해설을 모두 담는다.
 */
public record SimilarQuizSubmissionResult(
        Long similarQuizId,
        int score,
        int correctCount,
        int totalCount,
        List<Question> questions
) {
    public record Question(
            Long questionId,
            String content,
            List<String> options,
            int answerIndex,        // 정답 보기 순서(0-based)
            Integer selectedIndex,  // 내가 고른 보기 순서(0-based), 미응답 시 null
            String explanation,
            boolean correct
    ) {}
}
