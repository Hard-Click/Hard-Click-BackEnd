package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

public record SubmitQuizCommand(
        Long quizId,
        Long memberId,
        List<AnswerCommand> answers
) {
    public record AnswerCommand(Long questionId, Long selectedOptionId) {}
}
