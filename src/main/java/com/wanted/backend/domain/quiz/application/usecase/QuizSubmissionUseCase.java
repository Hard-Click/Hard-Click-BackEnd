package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.SubmitQuizCommand;
import com.wanted.backend.domain.quiz.application.result.QuizSubmissionResult;

public interface QuizSubmissionUseCase {

    QuizSubmissionResult submit(SubmitQuizCommand command);
}
