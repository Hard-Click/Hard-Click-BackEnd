package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;

public interface SimilarQuizUseCase {

    /** 강의(course) 오답 기반 유사문제 세트 생성. 오답이 없거나 유사문제가 없으면 null(정상 empty). */
    SimilarQuizResult generateForCourse(Long memberId, Long courseId);
}
