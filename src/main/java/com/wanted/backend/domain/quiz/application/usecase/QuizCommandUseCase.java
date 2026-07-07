package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;

public interface QuizCommandUseCase {

    Long create(CreateQuizCommand command);
}
