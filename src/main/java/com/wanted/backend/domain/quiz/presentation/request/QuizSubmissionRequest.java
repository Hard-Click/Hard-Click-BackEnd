package com.wanted.backend.domain.quiz.presentation.request;

import java.util.List;

public record QuizSubmissionRequest(List<Answer> answers) {
    public record Answer(Long questionId, Long selectedOptionId, Integer timeSpentSeconds) {
    }
}
