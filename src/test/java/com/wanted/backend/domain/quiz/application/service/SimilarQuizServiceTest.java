package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.SimilarProblemRecommenderPort;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmissionAnswer;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimilarQuizServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long COURSE_ID = 9001L;
    private static final Long QUIZ_ID = 90L;

    private QuizRepository quizRepository;
    private QuizSubmissionRepository quizSubmissionRepository;
    private SimilarProblemRecommenderPort recommender;
    private SimilarQuizRepository similarQuizRepository;
    private SimilarQuizSubscriptionAccessPort subscriptionAccessPort;
    private SimilarQuizService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        quizSubmissionRepository = mock(QuizSubmissionRepository.class);
        recommender = mock(SimilarProblemRecommenderPort.class);
        similarQuizRepository = mock(SimilarQuizRepository.class);
        subscriptionAccessPort = mock(SimilarQuizSubscriptionAccessPort.class);
        service = new SimilarQuizService(quizRepository, quizSubmissionRepository, recommender,
                similarQuizRepository, subscriptionAccessPort);
    }

    // 문항 10(오답 대상)과 20(유사문제 후보)을 가진 코스 퀴즈.
    private Quiz courseQuiz() {
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
        return Quiz.restore(QUIZ_ID, 1L, COURSE_ID, 100L, "국어", List.of(q10, q20),
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    // 문항 10을 틀린 제출.
    private QuizSubmission wrongOnQuestion10() {
        return QuizSubmission.restore(55L, QUIZ_ID, MEMBER_ID, 0, 1, 0,
                LocalDateTime.of(2026, 5, 11, 9, 0),
                List.of(QuizSubmissionAnswer.restore(1L, 10L, 101L, false, null)));
    }

    private void stubSavePassthrough() {
        when(similarQuizRepository.save(any())).thenAnswer(invocation -> {
            SimilarQuiz s = invocation.getArgument(0);
            return SimilarQuiz.restore(123L, s.getMemberId(), s.getCourseId(), s.getWeek(),
                    s.getTitle(), s.getQuestionIds(), s.getCreatedAt());
        });
    }

    @Test
    void generatePersistsSetAndReturnsQuestionsWithoutAnswers() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(courseQuiz()));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(wrongOnQuestion10()));
        // 원문제(10, 자기 자신)는 제외되고 20만 유사문제로 조립되어야 한다.
        when(recommender.recommendSimilar(MEMBER_ID, 10L, 2)).thenReturn(List.of(10L, 20L));
        stubSavePassthrough();

        SimilarQuizResult result = service.generateForCourse(MEMBER_ID, COURSE_ID, 3);

        assertThat(result).isNotNull();
        assertThat(result.similarQuizId()).isEqualTo(123L);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.week()).isEqualTo(3);
        assertThat(result.title()).isEqualTo("3주차 오답 유사 퀴즈");
        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).questionId()).isEqualTo(20L);
        assertThat(result.questions().get(0).options()).containsExactly("정답", "오답", "오답", "오답");
        verify(similarQuizRepository).save(any());
    }

    @Test
    void generateRejectsWhenNotSubscribed() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.generateForCourse(MEMBER_ID, COURSE_ID, 3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        verify(similarQuizRepository, never()).save(any());
    }

    @Test
    void generateReturnsNullWhenNoWrongAnswers() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(courseQuiz()));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of());

        assertThat(service.generateForCourse(MEMBER_ID, COURSE_ID, 3)).isNull();
        verify(similarQuizRepository, never()).save(any());
    }

    @Test
    void generateReturnsNullWhenNoSimilarWithinCourse() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(courseQuiz()));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(wrongOnQuestion10()));
        // 추천 결과가 코스 밖 문항(999)뿐 → 조립 결과 없음.
        when(recommender.recommendSimilar(MEMBER_ID, 10L, 2)).thenReturn(List.of(999L));

        assertThat(service.generateForCourse(MEMBER_ID, COURSE_ID, 3)).isNull();
        verify(similarQuizRepository, never()).save(any());
    }
}
