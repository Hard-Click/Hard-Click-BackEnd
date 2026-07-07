package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

public record CreateQuizCommand(
        Long instructorId,
        Long courseId,
        Long sectionId,
        String quizTitle,
        List<QuestionCommand> questions
) {
    public record QuestionCommand(
            String questionText,
            String explanation,
            int correctOptionNumber,
            List<String> optionTexts
    ) {}
}
