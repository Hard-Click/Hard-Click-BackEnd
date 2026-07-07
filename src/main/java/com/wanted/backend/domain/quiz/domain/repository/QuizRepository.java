package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.Quiz;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {

    Quiz save(Quiz quiz);

    List<Quiz> findAllByInstructor(Long instructorId, Long courseId, Long sectionId);

    Optional<Quiz> findById(Long id);

    Quiz update(Quiz quiz);

    void deleteById(Long id);
}
