package com.wanted.backend.domain.quiz.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizSubmissionTest {

    // 2문항 퀴즈: Q1(id10) 정답=보기2(id102), Q2(id20) 정답=보기1(id201)
    private Quiz twoQuestionQuiz() {
        QuizQuestion q1 = QuizQuestion.restore(10L, 1, "질문1", "해설1", List.of(
                QuizOption.restore(101L, 1, "오답", false),
                QuizOption.restore(102L, 2, "정답", true),
                QuizOption.restore(103L, 3, "오답", false),
                QuizOption.restore(104L, 4, "오답", false)));
        QuizQuestion q2 = QuizQuestion.restore(20L, 2, "질문2", "해설2", List.of(
                QuizOption.restore(201L, 1, "정답", true),
                QuizOption.restore(202L, 2, "오답", false),
                QuizOption.restore(203L, 3, "오답", false),
                QuizOption.restore(204L, 4, "오답", false)));
        return Quiz.restore(90L, 1L, 10L, 100L, "퀴즈", List.of(q1, q2),
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void gradeScoresAllCorrectAsHundred() {
        Quiz quiz = twoQuestionQuiz();

        QuizSubmission submission = QuizSubmission.grade(7L, quiz, Map.of(10L, 102L, 20L, 201L));

        assertThat(submission.getMemberId()).isEqualTo(7L);
        assertThat(submission.getQuizId()).isEqualTo(90L);
        assertThat(submission.getTotalQuestionCount()).isEqualTo(2);
        assertThat(submission.getCorrectCount()).isEqualTo(2);
        assertThat(submission.getScore()).isEqualTo(100);
        assertThat(submission.getIncorrectCount()).isZero();
        assertThat(submission.getAnswers()).allMatch(QuizSubmissionAnswer::isCorrect);
    }

    @Test
    void gradeMarksWrongAndUnansweredQuestionsIncorrect() {
        Quiz quiz = twoQuestionQuiz();

        // Q1은 오답(103) 선택, Q2는 미응답(맵에 없음)
        QuizSubmission submission = QuizSubmission.grade(7L, quiz, Map.of(10L, 103L));

        assertThat(submission.getCorrectCount()).isZero();
        assertThat(submission.getScore()).isZero();
        assertThat(submission.getIncorrectCount()).isEqualTo(2);
        QuizSubmissionAnswer q2Answer = submission.getAnswers().stream()
                .filter(a -> a.getQuestionId().equals(20L)).findFirst().orElseThrow();
        assertThat(q2Answer.getSelectedOptionId()).isNull();
        assertThat(q2Answer.isCorrect()).isFalse();
    }

    @Test
    void gradeRoundsPartialScore() {
        Quiz quiz = twoQuestionQuiz();

        // 2문항 중 1개 정답 → 50점
        QuizSubmission submission = QuizSubmission.grade(7L, quiz, Map.of(10L, 102L, 20L, 202L));

        assertThat(submission.getCorrectCount()).isEqualTo(1);
        assertThat(submission.getScore()).isEqualTo(50);
    }

    @Test
    void gradeRejectsNullMemberOrEmptyQuiz() {
        Quiz quiz = twoQuestionQuiz();

        assertThatThrownBy(() -> QuizSubmission.grade(null, quiz, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizSubmission.grade(7L, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gradeTreatsNullAnswerMapAsAllUnanswered() {
        Quiz quiz = twoQuestionQuiz();

        QuizSubmission submission = QuizSubmission.grade(7L, quiz, null);

        assertThat(submission.getCorrectCount()).isZero();
        assertThat(submission.getScore()).isZero();
        assertThat(submission.getAnswers()).hasSize(2);
    }
}
