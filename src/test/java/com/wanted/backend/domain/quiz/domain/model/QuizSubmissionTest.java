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

    // 3문항 퀴즈: Q1(id10) 정답=id102, Q2(id20) 정답=id201, Q3(id30) 정답=id301
    private Quiz threeQuestionQuiz() {
        QuizQuestion q1 = QuizQuestion.restore(10L, 1, "질문1", null, List.of(
                QuizOption.restore(101L, 1, "오답", false),
                QuizOption.restore(102L, 2, "정답", true),
                QuizOption.restore(103L, 3, "오답", false),
                QuizOption.restore(104L, 4, "오답", false)));
        QuizQuestion q2 = QuizQuestion.restore(20L, 2, "질문2", null, List.of(
                QuizOption.restore(201L, 1, "정답", true),
                QuizOption.restore(202L, 2, "오답", false),
                QuizOption.restore(203L, 3, "오답", false),
                QuizOption.restore(204L, 4, "오답", false)));
        QuizQuestion q3 = QuizQuestion.restore(30L, 3, "질문3", null, List.of(
                QuizOption.restore(301L, 1, "정답", true),
                QuizOption.restore(302L, 2, "오답", false),
                QuizOption.restore(303L, 3, "오답", false),
                QuizOption.restore(304L, 4, "오답", false)));
        return Quiz.restore(90L, 1L, 10L, 100L, "퀴즈", List.of(q1, q2, q3),
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void gradeRoundsPartialScoreDown() {
        Quiz quiz = threeQuestionQuiz();

        // 3문항 중 1개 정답 → 33.33 → 반올림 33
        QuizSubmission submission = QuizSubmission.grade(7L, quiz, Map.of(10L, 102L));

        assertThat(submission.getCorrectCount()).isEqualTo(1);
        assertThat(submission.getScore()).isEqualTo(33);
    }

    @Test
    void gradeRoundsPartialScoreUp() {
        Quiz quiz = threeQuestionQuiz();

        // 3문항 중 2개 정답 → 66.67 → 반올림 67
        QuizSubmission submission = QuizSubmission.grade(7L, quiz, Map.of(10L, 102L, 20L, 201L));

        assertThat(submission.getCorrectCount()).isEqualTo(2);
        assertThat(submission.getScore()).isEqualTo(67);
    }

    @Test
    void gradeRejectsNullMemberNullQuizOrEmptyQuestions() {
        Quiz quiz = twoQuestionQuiz();
        Quiz emptyQuiz = Quiz.restore(91L, 1L, 10L, 100L, "빈 퀴즈", List.of(),
                LocalDateTime.of(2026, 5, 10, 15, 30));

        assertThatThrownBy(() -> QuizSubmission.grade(null, quiz, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizSubmission.grade(7L, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizSubmission.grade(7L, emptyQuiz, Map.of()))
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

    @Test
    void gradeCarriesTimeSpentSecondsPerQuestion() {
        Quiz quiz = twoQuestionQuiz();

        // Q1은 70초 측정, Q2는 미측정(맵에 없음) → null
        QuizSubmission submission = QuizSubmission.grade(7L, quiz,
                Map.of(10L, 102L, 20L, 201L), Map.of(10L, 70));

        assertThat(answerOf(submission, 10L).getTimeSpentSeconds()).isEqualTo(70);
        assertThat(answerOf(submission, 20L).getTimeSpentSeconds()).isNull();
    }

    @Test
    void gradeNullifiesOutOfRangeTimeSpentSeconds() {
        Quiz quiz = threeQuestionQuiz();

        // 음수·상한 초과는 신뢰 불가 → null(미측정), 경계값 3600은 유지
        QuizSubmission submission = QuizSubmission.grade(7L, quiz,
                Map.of(10L, 102L, 20L, 201L, 30L, 301L),
                Map.of(10L, -5, 20L, 3601, 30L, 3600));

        assertThat(answerOf(submission, 10L).getTimeSpentSeconds()).isNull();
        assertThat(answerOf(submission, 20L).getTimeSpentSeconds()).isNull();
        assertThat(answerOf(submission, 30L).getTimeSpentSeconds()).isEqualTo(3600);
    }

    private QuizSubmissionAnswer answerOf(QuizSubmission submission, Long questionId) {
        return submission.getAnswers().stream()
                .filter(a -> a.getQuestionId().equals(questionId)).findFirst().orElseThrow();
    }
}
