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
    void createRejectsABlankTitle() {
        QuizQuestion question = QuizQuestion.create(1, "질문", null, 1,
                List.of("보기1", "보기2", "보기3", "보기4"));

        assertThatThrownBy(() -> Quiz.create(1L, 10L, 100L, " ", List.of(question)))
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
