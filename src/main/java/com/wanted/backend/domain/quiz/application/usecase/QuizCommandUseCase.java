package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;

public interface QuizCommandUseCase {

    Long create(CreateQuizCommand command);

    Long update(UpdateQuizCommand command);

    void delete(DeleteQuizCommand command);
}
