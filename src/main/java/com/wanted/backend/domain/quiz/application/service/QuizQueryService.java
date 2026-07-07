package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQueryService implements QuizQueryUseCase {

    private final QuizRepository quizRepository;
    private final CourseTitlePort courseTitlePort;
    private final CourseSectionTitlePort courseSectionTitlePort;

    @Override
    public List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId) {
        List<Quiz> quizzes = quizRepository.findAllByInstructor(instructorId, courseId, sectionId);

        Map<Long, String> courseTitles = courseTitlePort.findTitlesByCourseIds(
                quizzes.stream().map(Quiz::getCourseId).distinct().toList());

        Map<Long, String> sectionTitles = courseSectionTitlePort.findTitlesBySectionIds(
                quizzes.stream().map(Quiz::getSectionId).distinct().toList());

        return quizzes.stream()
                .map(quiz -> new InstructorQuizSummary(
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourseId(),
                        courseTitles.getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId()),
                        quiz.getSectionId(),
                        sectionTitles.getOrDefault(quiz.getSectionId(), "섹션 #" + quiz.getSectionId()),
                        quiz.getQuestions().size(),
                        quiz.getCreatedAt()))
                .toList();
    }
}
