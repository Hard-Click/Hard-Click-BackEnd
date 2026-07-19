package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilarQuizSubmissionServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long OTHER_MEMBER_ID = 8L;
    private static final Long COURSE_ID = 9001L;
    private static final Long SIMILAR_QUIZ_ID = 123L;

    private SimilarQuizRepository similarQuizRepository;
    private QuizRepository quizRepository;
    private SimilarQuizSubscriptionAccessPort subscriptionAccessPort;
    private SimilarQuizSubmissionService service;

    @BeforeEach
    void setUp() {
        similarQuizRepository = mock(SimilarQuizRepository.class);
        quizRepository = mock(QuizRepository.class);
        subscriptionAccessPort = mock(SimilarQuizSubscriptionAccessPort.class);
        service = new SimilarQuizSubmissionService(similarQuizRepository, quizRepository, subscriptionAccessPort);
    }

    // 문항 10(정답=보기2, answerIndex 1)·20(정답=보기1, answerIndex 0) — 유사퀴즈가 참조하는 원문항.
    private List<QuizQuestion> courseQuestions() {
        QuizQuestion q10 = QuizQuestion.restore(10L, 1, "질문10", "해설10", List.of(
                QuizOption.restore(101L, 1, "오답", false),
                QuizOption.restore(102L, 2, "정답", true),
                QuizOption.restore(103L, 3, "오답", false),
                QuizOption.restore(104L, 4, "오답", false)));
        QuizQuestion q20 = QuizQuestion.restore(20L, 2, "질문20", "해설20", List.of(
                QuizOption.restore(201L, 1, "정답", true),
                QuizOption.restore(202L, 2, "오답", false),
                QuizOption.restore(203L, 3, "오답", false),
                QuizOption.restore(204L, 4, "오답", false)));
        return List.of(q10, q20);
    }

    private SimilarQuiz similarQuiz(Long ownerId) {
        return SimilarQuiz.restore(SIMILAR_QUIZ_ID, ownerId, COURSE_ID, 3, "3주차 오답 유사 퀴즈",
                List.of(10L, 20L), LocalDateTime.of(2026, 5, 12, 10, 0));
    }

    @Test
    void submitGradesWithAnswersExplanationsAndScore() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(10L, 20L))).thenReturn(courseQuestions());

        // 10번은 정답(index 1), 20번은 오답(index 2, 정답 index 0)
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(10L, 1),
                new SubmitSimilarQuizCommand.AnswerCommand(20L, 2)));

        SimilarQuizSubmissionResult result = service.submit(command);

        assertThat(result.similarQuizId()).isEqualTo(SIMILAR_QUIZ_ID);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(50);

        SimilarQuizSubmissionResult.Question q10 = result.questions().get(0);
        assertThat(q10.questionId()).isEqualTo(10L);
        assertThat(q10.answerIndex()).isEqualTo(1);
        assertThat(q10.selectedIndex()).isEqualTo(1);
        assertThat(q10.correct()).isTrue();
        assertThat(q10.explanation()).isEqualTo("해설10");

        SimilarQuizSubmissionResult.Question q20 = result.questions().get(1);
        assertThat(q20.answerIndex()).isZero();
        assertThat(q20.selectedIndex()).isEqualTo(2);
        assertThat(q20.correct()).isFalse();
    }

    @Test
    void submitTreatsUnansweredQuestionAsIncorrectWithNullSelectedIndex() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(10L, 20L))).thenReturn(courseQuestions());

        // 10번만 정답 제출, 20번 미응답
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(10L, 1)));

        SimilarQuizSubmissionResult result = service.submit(command);

        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(50);
        SimilarQuizSubmissionResult.Question q20 = result.questions().get(1);
        assertThat(q20.selectedIndex()).isNull();
        assertThat(q20.correct()).isFalse();
    }

    @Test
    void submitRejectsWhenNotSubscribed() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(false);

        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of());

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
    }

    @Test
    void submitRejectsWhenSimilarQuizNotFound() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.empty());

        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of());

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_NOT_FOUND);
    }

    @Test
    void submitRejectsWhenNotOwner() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        // 다른 회원 소유 세트 — 존재 여부를 노출하지 않도록 동일하게 404 처리.
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(OTHER_MEMBER_ID)));

        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of());

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_NOT_FOUND);
    }
}
