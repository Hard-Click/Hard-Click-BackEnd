package com.wanted.backend.domain.quiz.domain.event;

import com.wanted.backend.global.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

/**
 * 학생이 퀴즈를 제출하고 채점이 끝났을 때 발행된다.
 * AI(Python) 서버로 제출 내용을 비동기 전송하는 트리거로 사용된다.
 */
public record QuizSubmittedEvent(
        Long submissionId,
        Long quizId,
        Long memberId,
        int score,
        int totalQuestionCount,
        int correctCount,
        List<AnsweredQuestion> answers,
        Instant occurredAt
) implements DomainEvent {

    public record AnsweredQuestion(Long questionId, Long selectedOptionId, boolean correct) {}

    public static QuizSubmittedEvent of(Long submissionId, Long quizId, Long memberId, int score,
                                         int totalQuestionCount, int correctCount,
                                         List<AnsweredQuestion> answers) {
        return new QuizSubmittedEvent(submissionId, quizId, memberId, score,
                totalQuestionCount, correctCount, answers, Instant.now());
    }
}
