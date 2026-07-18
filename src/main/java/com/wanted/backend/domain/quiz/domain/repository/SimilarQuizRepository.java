package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;

import java.util.Optional;

/**
 * 유사퀴즈 생성 세트 영속 포트.
 * 생성(①) 시 저장하고, 제출(②) 채점 시 id로 복원한다.
 */
public interface SimilarQuizRepository {

    SimilarQuiz save(SimilarQuiz similarQuiz);

    Optional<SimilarQuiz> findById(Long id);
}
