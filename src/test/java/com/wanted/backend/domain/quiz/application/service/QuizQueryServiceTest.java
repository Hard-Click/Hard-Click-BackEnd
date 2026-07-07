package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuizQueryServiceTest {

    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long SECTION_ID = 100L;

    private QuizRepository quizRepository;
    private CourseTitlePort courseTitlePort;
    private CourseSectionTitlePort courseSectionTitlePort;
    private QuizQueryService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        courseTitlePort = mock(CourseTitlePort.class);
        courseSectionTitlePort = mock(CourseSectionTitlePort.class);
        service = new QuizQueryService(quizRepository, courseTitlePort, courseSectionTitlePort);
    }

    private Quiz quiz(Long id, Long courseId, Long sectionId, String title, int questionCount) {
        List<QuizQuestion> questions = new java.util.ArrayList<>();
        for (int i = 1; i <= questionCount; i++) {
            questions.add(QuizQuestion.create(i, "질문" + i, null, 1,
                    List.of("보기1", "보기2", "보기3", "보기4")));
        }
        return Quiz.restore(id, INSTRUCTOR_ID, courseId, sectionId, title, questions,
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void instructorQuizzesIncludeCourseAndSectionTitlesAndQuestionCounts() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, COURSE_ID, null)).thenReturn(List.of(
                quiz(90L, COURSE_ID, SECTION_ID, "1주차 퀴즈", 8),
                quiz(91L, COURSE_ID, 101L, "2주차 퀴즈", 5)
        ));
        when(courseTitlePort.findTitleByCourseId(COURSE_ID)).thenReturn(Optional.of("React 완벽 가이드"));
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, "섹션 1: React 기초", 101L, "섹션 2: Hooks"));

        List<InstructorQuizSummary> summaries = service.getInstructorQuizzes(INSTRUCTOR_ID, COURSE_ID, null);

        assertThat(summaries).hasSize(2);
        InstructorQuizSummary first = summaries.get(0);
        assertThat(first.quizId()).isEqualTo(90L);
        assertThat(first.quizTitle()).isEqualTo("1주차 퀴즈");
        assertThat(first.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(first.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(first.questionCount()).isEqualTo(8);
        assertThat(summaries.get(1).sectionTitle()).isEqualTo("섹션 2: Hooks");
        assertThat(summaries.get(1).questionCount()).isEqualTo(5);
    }

    @Test
    void instructorQuizzesFallBackToPlaceholderTitlesWhenReferencesAreMissing() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, null, null))
                .thenReturn(List.of(quiz(90L, 999L, 888L, "퀴즈", 1)));
        when(courseTitlePort.findTitleByCourseId(999L)).thenReturn(Optional.empty());
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection())).thenReturn(Map.of());

        List<InstructorQuizSummary> summaries = service.getInstructorQuizzes(INSTRUCTOR_ID, null, null);

        assertThat(summaries.get(0).courseTitle()).isEqualTo("강의 #999");
        assertThat(summaries.get(0).sectionTitle()).isEqualTo("섹션 #888");
    }

    @Test
    void instructorQuizzesReturnAnEmptyListWhenTheInstructorHasNoQuizzes() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, null, null)).thenReturn(List.of());
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection())).thenReturn(Map.of());

        assertThat(service.getInstructorQuizzes(INSTRUCTOR_ID, null, null)).isEmpty();
    }
}
