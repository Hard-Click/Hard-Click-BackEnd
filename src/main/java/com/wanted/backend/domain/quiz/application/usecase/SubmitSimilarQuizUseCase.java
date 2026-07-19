package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;

public interface SubmitSimilarQuizUseCase {

    /** 유사퀴즈 답안을 채점하고 정답·내답·해설을 포함한 결과를 반환한다. 구독 회원·본인 생성 세트만 가능. */
    SimilarQuizSubmissionResult submit(SubmitSimilarQuizCommand command);
}
