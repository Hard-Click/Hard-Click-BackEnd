package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
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
        when(courseTitlePort.findTitlesByCourseIds(anyCollection())).thenReturn(Map.of());
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

    @Test
    void instructorQuizDetailReturnsQuestionsWithCorrectOptionResolvedFromTheDomain() {
        // 실제 조회 시나리오처럼 DB에서 복원된(restore) 엔티티 — 문항/보기 id가 채워진 상태
        QuizQuestion question = QuizQuestion.restore(11L, 1, "React의 가상 DOM이란?", "가상 DOM 설명",
                List.of(
                        QuizOption.restore(21L, 1, "보기1", false),
                        QuizOption.restore(22L, 2, "보기2", true),
                        QuizOption.restore(23L, 3, "보기3", false),
                        QuizOption.restore(24L, 4, "보기4", false)));
        Quiz quiz = Quiz.restore(90L, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "React 기초 개념 퀴즈",
                List.of(question), LocalDateTime.of(2026, 5, 10, 15, 30));
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, "섹션 1: React 기초"));

        InstructorQuizDetail detail = service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L);

        assertThat(detail.quizId()).isEqualTo(90L);
        assertThat(detail.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(detail.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(detail.questionCount()).isEqualTo(1);
        InstructorQuizDetail.QuestionDetail questionDetail = detail.questions().get(0);
        assertThat(questionDetail.questionId()).isEqualTo(11L);
        assertThat(questionDetail.explanation()).isEqualTo("가상 DOM 설명");
        assertThat(questionDetail.options()).hasSize(4);
        // 두 번째 보기(id 22)가 정답 → correctOptionId가 그 id와 일치
        assertThat(questionDetail.correctOptionId()).isEqualTo(22L);
        assertThat(questionDetail.options().get(1).correct()).isTrue();
    }

    @Test
    void instructorQuizDetailFallsBackToPlaceholderTitlesWhenReferencesAreMissing() {
        Quiz quiz = Quiz.restore(90L, INSTRUCTOR_ID, 999L, 888L, "퀴즈",
                List.of(QuizQuestion.restore(1L, 1, "질문", null,
                        List.of(
                                QuizOption.restore(1L, 1, "보기1", true),
                                QuizOption.restore(2L, 2, "보기2", false),
                                QuizOption.restore(3L, 3, "보기3", false),
                                QuizOption.restore(4L, 4, "보기4", false)))),
                LocalDateTime.of(2026, 5, 10, 15, 30));
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection())).thenReturn(Map.of());
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection())).thenReturn(Map.of());

        InstructorQuizDetail detail = service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L);

        assertThat(detail.courseTitle()).isEqualTo("강의 #999");
        assertThat(detail.sectionTitle()).isEqualTo("섹션 #888");
    }

    @Test
    void instructorQuizDetailRejectsWhenTheQuizDoesNotExist() {
        when(quizRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void instructorQuizDetailRejectsWhenTheQuizBelongsToAnotherInstructor() {
        Quiz quiz = quiz(90L, COURSE_ID, SECTION_ID, "퀴즈", 1);
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> service.getInstructorQuizDetail(999L, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }
}
