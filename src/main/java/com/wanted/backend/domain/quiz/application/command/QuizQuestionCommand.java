package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

public record QuizQuestionCommand(
        String questionText,
        String explanation,
        Integer difficulty,
        int correctOptionNumber,
        List<String> optionTexts
) {}
