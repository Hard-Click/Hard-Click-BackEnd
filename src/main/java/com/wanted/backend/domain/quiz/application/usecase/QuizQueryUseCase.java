package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;

import java.util.List;

public interface QuizQueryUseCase {

    List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId);
}
