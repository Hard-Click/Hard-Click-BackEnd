package com.wanted.backend.domain.quiz.application.command;

public record DeleteQuizCommand(
        Long quizId,
        Long instructorId
) {}
