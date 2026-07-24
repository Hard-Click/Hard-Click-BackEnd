package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.port.ReviewCompletionPort;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SimilarQuizSubmissionServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long OTHER_MEMBER_ID = 8L;
    private static final Long COURSE_ID = 9001L;
    private static final Long SIMILAR_QUIZ_ID = 123L;

    private SimilarQuizRepository similarQuizRepository;
    private QuizRepository quizRepository;
    private SimilarQuizSubmissionRepository submissionRepository;
    private SimilarQuizSubscriptionAccessPort subscriptionAccessPort;
    private ReviewCompletionPort reviewCompletionPort;
    private SimilarQuizSubmissionService service;

    @BeforeEach
    void setUp() {
        similarQuizRepository = mock(SimilarQuizRepository.class);
        quizRepository = mock(QuizRepository.class);
        submissionRepository = mock(SimilarQuizSubmissionRepository.class);
        subscriptionAccessPort = mock(SimilarQuizSubscriptionAccessPort.class);
        reviewCompletionPort = mock(ReviewCompletionPort.class);
        service = new SimilarQuizSubmissionService(
                similarQuizRepository, quizRepository, submissionRepository, subscriptionAccessPort,
                reviewCompletionPort);
    }

    // 유사퀴즈가 참조하는 원문항 두 개. 정답 위치(answerIndex)를 인자로 명시한다.
    private static final long Q1_ID = 10L;   // 정답 = 보기 index 1
    private static final int  Q1_ANSWER_INDEX = 1;
    private static final long Q2_ID = 20L;   // 정답 = 보기 index 0
    private static final int  Q2_ANSWER_INDEX = 0;

    private List<QuizQuestion> courseQuestions() {
        return List.of(fourChoiceQuestion(Q1_ID, Q1_ANSWER_INDEX), fourChoiceQuestion(Q2_ID, Q2_ANSWER_INDEX));
    }

    // 정답 위치(answerIndex)가 아닌 유효한 오답 보기 위치(0~3). 시나리오의 "오답 선택"을 상수에서 파생해
    // 정답 인덱스를 바꿔도 오답 시나리오가 자동으로 따라오게 한다(매직 리터럴 제거).
    private static int wrongIndex(int answerIndex) {
        return answerIndex == 0 ? 1 : 0;
    }

    // 4지선다 문항 생성 — correctIndex(0~3) 위치의 보기만 정답. 보기 id는 문항 id에서 파생해 고유성 보장.
    private QuizQuestion fourChoiceQuestion(long questionId, int correctIndex) {
        List<QuizOption> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            boolean correct = (i == correctIndex);
            options.add(QuizOption.restore(questionId * 10 + i, i + 1, correct ? "정답" : "오답", correct));
        }
        return QuizQuestion.restore(questionId, 1, "질문" + questionId, "해설" + questionId, options);
    }

    private SimilarQuiz similarQuiz(Long ownerId) {
        return SimilarQuiz.restore(SIMILAR_QUIZ_ID, ownerId, COURSE_ID, 3, "3주차 오답 유사 퀴즈",
                List.of(Q1_ID, Q2_ID), LocalDateTime.of(2026, 5, 12, 10, 0));
    }

    @Test
    void submitGradesWithAnswersExplanationsAndScore() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        // Q1은 정답 선택, Q2는 오답 선택
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, 70),
                new SubmitSimilarQuizCommand.AnswerCommand(Q2_ID, wrongIndex(Q2_ANSWER_INDEX), null)));

        SimilarQuizSubmissionResult result = service.submit(command);

        assertThat(result.similarQuizId()).isEqualTo(SIMILAR_QUIZ_ID);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(50);

        SimilarQuizSubmissionResult.Question q1 = result.questions().get(0);
        assertThat(q1.questionId()).isEqualTo(Q1_ID);
        assertThat(q1.answerIndex()).isEqualTo(Q1_ANSWER_INDEX);
        assertThat(q1.selectedIndex()).isEqualTo(Q1_ANSWER_INDEX);
        assertThat(q1.correct()).isTrue();
        assertThat(q1.explanation()).isEqualTo("해설" + Q1_ID);

        SimilarQuizSubmissionResult.Question q2 = result.questions().get(1);
        assertThat(q2.answerIndex()).isEqualTo(Q2_ANSWER_INDEX);
        assertThat(q2.selectedIndex()).isEqualTo(wrongIndex(Q2_ANSWER_INDEX));
        assertThat(q2.correct()).isFalse();
    }

    @Test
    void submitTreatsUnansweredQuestionAsIncorrectWithNullSelectedIndex() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        // Q1만 정답 제출, Q2 미응답
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, null)));

        SimilarQuizSubmissionResult result = service.submit(command);

        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(50);
        SimilarQuizSubmissionResult.Question q2 = result.questions().get(1);
        assertThat(q2.selectedIndex()).isNull();
        assertThat(q2.correct()).isFalse();
    }

    @Test
    void submitPersistsSubmissionWithPerQuestionTimeAndCorrectness() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        // Q1 정답+70초, Q2 오답+시간 미측정(null)
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, 70),
                new SubmitSimilarQuizCommand.AnswerCommand(Q2_ID, wrongIndex(Q2_ANSWER_INDEX), null)));

        service.submit(command);

        ArgumentCaptor<SimilarQuizSubmission> captor = ArgumentCaptor.forClass(SimilarQuizSubmission.class);
        verify(submissionRepository).save(captor.capture());
        SimilarQuizSubmission saved = captor.getValue();
        assertThat(saved.getSimilarQuizId()).isEqualTo(SIMILAR_QUIZ_ID);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getCorrectCount()).isEqualTo(1);

        SimilarQuizSubmission.Answer a1 = saved.getAnswers().stream()
                .filter(a -> a.getQuestionId().equals(Q1_ID)).findFirst().orElseThrow();
        assertThat(a1.getTimeSpentSeconds()).isEqualTo(70);
        assertThat(a1.isCorrect()).isTrue();

        SimilarQuizSubmission.Answer a2 = saved.getAnswers().stream()
                .filter(a -> a.getQuestionId().equals(Q2_ID)).findFirst().orElseThrow();
        assertThat(a2.getTimeSpentSeconds()).isNull();
        assertThat(a2.isCorrect()).isFalse();
    }

    @Test
    void submitNullifiesOutOfRangeTimeSpent() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        // 음수·상한 초과(3600s 초과)는 신뢰 불가 → null(미측정). 보기 인덱스는 이 테스트와 무관해 정답 위치로 채운다.
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, -5),
                new SubmitSimilarQuizCommand.AnswerCommand(Q2_ID, Q2_ANSWER_INDEX, 3601)));

        service.submit(command);

        ArgumentCaptor<SimilarQuizSubmission> captor = ArgumentCaptor.forClass(SimilarQuizSubmission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().getAnswers()).allMatch(a -> a.getTimeSpentSeconds() == null);
    }

    @Test
    void submitKeepsBoundaryTimeSpent() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        // 경계값: 0초(즉답도 진짜값)와 3600초(상한 포함)는 유지. 초과(3601+)만 이상치로 null 처리하므로
        // normalizeTimeSpent 의 > 가 >= 로, < 0 이 <= 0 으로 잘못 리팩터되면 이 두 값이 깨진다 — 그 회귀를 막는다.
        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, 0),
                new SubmitSimilarQuizCommand.AnswerCommand(Q2_ID, Q2_ANSWER_INDEX, 3600)));

        service.submit(command);

        ArgumentCaptor<SimilarQuizSubmission> captor = ArgumentCaptor.forClass(SimilarQuizSubmission.class);
        verify(submissionRepository).save(captor.capture());
        SimilarQuizSubmission saved = captor.getValue();
        assertThat(timeSpentOf(saved, Q1_ID)).isEqualTo(0);
        assertThat(timeSpentOf(saved, Q2_ID)).isEqualTo(3600);
    }

    private static Integer timeSpentOf(SimilarQuizSubmission submission, long questionId) {
        return submission.getAnswers().stream()
                .filter(a -> a.getQuestionId().equals(questionId)).findFirst().orElseThrow()
                .getTimeSpentSeconds();
    }

    @Test
    void submitCompletesTodayReviewsForThatCourse() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(true);
        when(similarQuizRepository.findById(SIMILAR_QUIZ_ID)).thenReturn(Optional.of(similarQuiz(MEMBER_ID)));
        when(quizRepository.findQuestionsByIds(List.of(Q1_ID, Q2_ID))).thenReturn(courseQuestions());

        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of(
                new SubmitSimilarQuizCommand.AnswerCommand(Q1_ID, Q1_ANSWER_INDEX, 30)));

        service.submit(command);

        // 제출 성공 = 그 코스의 오늘 복습 완료 → 복습 카드 전진 트리거(member·course 전달).
        verify(reviewCompletionPort).completeTodayReviews(eq(MEMBER_ID), eq(COURSE_ID), any());
    }

    @Test
    void submitRejectsWhenNotSubscribed() {
        when(subscriptionAccessPort.hasActiveSubscription(MEMBER_ID)).thenReturn(false);

        SubmitSimilarQuizCommand command = new SubmitSimilarQuizCommand(SIMILAR_QUIZ_ID, MEMBER_ID, List.of());

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        verifyNoInteractions(submissionRepository); // 거부 시 이력 저장 안 함
        verifyNoInteractions(reviewCompletionPort); // 거부 시 복습 완료 트리거 안 함
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
