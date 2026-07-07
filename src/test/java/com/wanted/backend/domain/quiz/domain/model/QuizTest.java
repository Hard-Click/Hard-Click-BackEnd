package com.wanted.backend.domain.quiz.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizTest {

    @Test
    void createBuildsAQuizWithQuestionsAndOptions() {
        QuizQuestion question = QuizQuestion.create(1, "React의 가상 DOM이란?", "설명", 2,
                List.of("보기1", "보기2", "보기3", "보기4"));

        Quiz quiz = Quiz.create(1L, 10L, 100L, "React 기초 퀴즈", List.of(question));

        assertThat(quiz.getTitle()).isEqualTo("React 기초 퀴즈");
        assertThat(quiz.getQuestions()).hasSize(1);
        assertThat(quiz.getQuestions().get(0).getOptions()).hasSize(4);
        assertThat(quiz.getQuestions().get(0).getOptions().get(1).isCorrect()).isTrue();
    }

    @Test
    void createRejectsAQuizWithNoQuestions() {
        assertThatThrownBy(() -> Quiz.create(1L, 10L, 100L, "제목", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsMissingInstructorCourseOrSectionIds() {
        QuizQuestion question = QuizQuestion.create(1, "질문", null, 1,
                List.of("보기1", "보기2", "보기3", "보기4"));

        assertThatThrownBy(() -> Quiz.create(null, 10L, 100L, "제목", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Quiz.create(1L, null, 100L, "제목", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Quiz.create(1L, 10L, null, "제목", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsABlankTitle() {
        QuizQuestion question = QuizQuestion.create(1, "질문", null, 1,
                List.of("보기1", "보기2", "보기3", "보기4"));

        assertThatThrownBy(() -> Quiz.create(1L, 10L, 100L, " ", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateReplacesCourseSectionTitleAndQuestions() {
        QuizQuestion original = QuizQuestion.create(1, "기존 질문", null, 1,
                List.of("보기1", "보기2", "보기3", "보기4"));
        Quiz quiz = Quiz.create(1L, 10L, 100L, "기존 제목", List.of(original));

        QuizQuestion replacement1 = QuizQuestion.create(1, "새 질문1", "설명", 2,
                List.of("보기1", "보기2", "보기3", "보기4"));
        QuizQuestion replacement2 = QuizQuestion.create(2, "새 질문2", null, 3,
                List.of("보기1", "보기2", "보기3", "보기4"));

        quiz.update(20L, 200L, "새 제목", List.of(replacement1, replacement2));

        assertThat(quiz.getCourseId()).isEqualTo(20L);
        assertThat(quiz.getSectionId()).isEqualTo(200L);
        assertThat(quiz.getTitle()).isEqualTo("새 제목");
        assertThat(quiz.getQuestions()).hasSize(2);
        assertThat(quiz.getQuestions().get(0).getQuestionText()).isEqualTo("새 질문1");
    }

    @Test
    void updateRejectsABlankTitleOrEmptyQuestions() {
        QuizQuestion question = QuizQuestion.create(1, "질문", null, 1,
                List.of("보기1", "보기2", "보기3", "보기4"));
        Quiz quiz = Quiz.create(1L, 10L, 100L, "제목", List.of(question));

        assertThatThrownBy(() -> quiz.update(10L, 100L, " ", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quiz.update(10L, 100L, "제목", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quiz.update(null, 100L, "제목", List.of(question)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void questionCreateRejectsWrongOptionCount() {
        assertThatThrownBy(() -> QuizQuestion.create(1, "질문", null, 1, List.of("보기1", "보기2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void questionCreateRejectsAnOutOfRangeCorrectOptionNumber() {
        assertThatThrownBy(() -> QuizQuestion.create(1, "질문", null, 5,
                List.of("보기1", "보기2", "보기3", "보기4")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
