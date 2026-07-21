package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

/**
 * 유사퀴즈 제출·채점 커맨드. selectedIndex는 보기 순서(0~3) 기준 — 미응답은 null 허용.
 */
public record SubmitSimilarQuizCommand(
        Long similarQuizId,
        Long memberId,
        List<AnswerCommand> answers
) {
    public record AnswerCommand(Long questionId, Integer selectedIndex, Integer timeSpentSeconds) {}
}
