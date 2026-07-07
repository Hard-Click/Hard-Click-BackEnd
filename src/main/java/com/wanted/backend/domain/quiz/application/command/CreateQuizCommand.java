package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

public record CreateQuizCommand(
        Long instructorId,
        Long courseId,
        Long sectionId,
        String quizTitle,
        List<QuizQuestionCommand> questions
) {}
