package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort;
import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort.ReviewItem;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewQuizServiceTest {

    private static final Long MEMBER_ID = 7L;

    private ReviewRecommenderPort recommender;
    private QuizRepository quizRepository;
    private SimilarQuizSubscriptionAccessPort subscriptionAccessPort;
    private ReviewQuizService service;

    @BeforeEach
    void setUp() {
        recommender = mock(ReviewRecommenderPort.class);
        quizRepository = mock(QuizRepository.class);
        subscriptionAccessPort = mock(SimilarQuizSubscriptionAccessPort.class);
        service = new ReviewQuizService(recommender, quizRepository, subscriptionAccessPort);
    }

    private QuizQuestion question(long id, String text) {
        return QuizQuestion.restore(id, 1, text, "해설" + id, List.of(
                QuizOption.restore(id * 10 + 1, 1, "정답", true),
                QuizOption.restore(id * 10 + 2, 2, "오답", false),
                QuizOption.restore(id * 10 + 3, 3, "오답", false),
                QuizOption.restore(id * 10 + 4, 4, "오답", false)));
    }

    @Test
    void generateGroupsSimilarQuestionsByOriginalAcrossCourses() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        // 추천기가 원문제 2개(서로 다른 섹션)와 각 유사문제 id를 급한 순으로 돌려준다.
        when(recommender.recommendReview(eq(MEMBER_ID), anyInt())).thenReturn(List.of(
                new ReviewItem(1101L, 137L, List.of(201L, 202L)),
                new ReviewItem(1130L, 139L, List.of(301L))));
        // 코스 경계 없이 by-id 로 조립한다(섹션/코스가 달라도 조회).
        when(quizRepository.findQuestionsByIds(anyList())).thenReturn(List.of(
                question(201L, "유사201"), question(202L, "유사202"), question(301L, "유사301")));

        ReviewQuizResult result = service.generateForStudent(MEMBER_ID);

        assertThat(result).isNotNull();
        assertThat(result.reviews()).hasSize(2);
        assertThat(result.reviews().get(0).originalQuestionId()).isEqualTo(1101L);
        assertThat(result.reviews().get(0).sectionId()).isEqualTo(137L);
        assertThat(result.reviews().get(0).similar())
                .extracting(ReviewQuizResult.Question::questionId)
                .containsExactly(201L, 202L);
        assertThat(result.reviews().get(1).originalQuestionId()).isEqualTo(1130L);
        assertThat(result.reviews().get(1).similar())
                .extracting(ReviewQuizResult.Question::questionId)
                .containsExactly(301L);
        assertThat(result.reviews().get(0).similar().get(0).options())
                .containsExactly("정답", "오답", "오답", "오답");
    }

    @Test
    void generateRejectsWhenNotSubscribed() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.generateForStudent(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        verify(recommender, never()).recommendReview(anyInt(), anyInt());
    }

    @Test
    void generateReturnsNullWhenNoHistory() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(recommender.recommendReview(eq(MEMBER_ID), anyInt())).thenReturn(List.of());

        assertThat(service.generateForStudent(MEMBER_ID)).isNull();
        verify(quizRepository, never()).findQuestionsByIds(anyList());
    }

    @Test
    void generateDeduplicatesRepeatedSimilarIdWithinSameGroup() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        // 추천기가 한 그룹 안에서 같은 유사문제 id를 중복으로 돌려줘도 응답엔 한 번만 실려야 한다.
        when(recommender.recommendReview(eq(MEMBER_ID), anyInt())).thenReturn(List.of(
                new ReviewItem(1101L, 137L, List.of(201L, 201L, 202L))));
        when(quizRepository.findQuestionsByIds(anyList())).thenReturn(List.of(
                question(201L, "유사201"), question(202L, "유사202")));

        ReviewQuizResult result = service.generateForStudent(MEMBER_ID);

        assertThat(result).isNotNull();
        assertThat(result.reviews()).hasSize(1);
        assertThat(result.reviews().get(0).similar())
                .extracting(ReviewQuizResult.Question::questionId)
                .containsExactly(201L, 202L);
    }

    @Test
    void generateReturnsNullWhenSimilarQuestionsNotFound() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(recommender.recommendReview(eq(MEMBER_ID), anyInt())).thenReturn(List.of(
                new ReviewItem(1101L, 137L, List.of(201L))));
        // 유사문제가 아직 인덱싱/조회 불가 → 조립 결과 없음 → null.
        when(quizRepository.findQuestionsByIds(anyList())).thenReturn(List.of());

        assertThat(service.generateForStudent(MEMBER_ID)).isNull();
    }
}
