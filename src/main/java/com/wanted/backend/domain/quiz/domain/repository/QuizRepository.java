package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.Quiz;

import java.util.Optional;

public interface QuizRepository {

    Quiz save(Quiz quiz);

    Optional<Quiz> findById(Long id);

    Quiz update(Quiz quiz);
}
