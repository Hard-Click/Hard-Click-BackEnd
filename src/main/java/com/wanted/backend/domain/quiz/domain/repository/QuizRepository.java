package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.Quiz;

public interface QuizRepository {

    Quiz save(Quiz quiz);
}
