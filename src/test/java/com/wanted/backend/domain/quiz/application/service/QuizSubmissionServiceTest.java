package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitQuizCommand;
import com.wanted.backend.domain.quiz.application.result.QuizSubmissionResult;
import com.wanted.backend.domain.quiz.domain.event.QuizSubmittedEvent;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizSubmissionServiceTest {

    private static final Long QUIZ_ID = 90L;
    private static final Long MEMBER_ID = 7L;

    private QuizRepository quizRepository;
    private QuizSubmissionRepository quizSubmissionRepository;
    private ApplicationEventPublisher eventPublisher;
    private QuizSubmissionService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        quizSubmissionRepository = mock(QuizSubmissionRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new QuizSubmissionService(quizRepository, quizSubmissionRepository, eventPublisher);
    }

    private Quiz quiz() {
        QuizQuestion q1 = QuizQuestion.restore(10L, 1, "질문1", "해설1", List.of(
                QuizOption.restore(101L, 1, "오답", false),
                QuizOption.restore(102L, 2, "정답", true),
                QuizOption.restore(103L, 3, "오답", false),
                QuizOption.restore(104L, 4, "오답", false)));
        return Quiz.restore(QUIZ_ID, 1L, 10L, 100L, "퀴즈", List.of(q1),
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void submitGradesSavesAndPublishesEvent() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz()));
        when(quizSubmissionRepository.existsByQuizIdAndMemberId(QUIZ_ID, MEMBER_ID)).thenReturn(false);
        when(quizSubmissionRepository.save(any())).thenAnswer(invocation -> {
            QuizSubmission s = invocation.getArgument(0);
            return QuizSubmission.restore(55L, s.getQuizId(), s.getMemberId(), s.getScore(),
                    s.getTotalQuestionCount(), s.getCorrectCount(), s.getSubmittedAt(), s.getAnswers());
        });

        SubmitQuizCommand command = new SubmitQuizCommand(QUIZ_ID, MEMBER_ID,
                List.of(new SubmitQuizCommand.AnswerCommand(10L, 102L)));

        QuizSubmissionResult result = service.submit(command);

        assertThat(result.submissionId()).isEqualTo(55L);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.incorrectCount()).isZero();

        verify(quizSubmissionRepository).save(any());
        ArgumentCaptor<QuizSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(QuizSubmittedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        QuizSubmittedEvent event = eventCaptor.getValue();
        assertThat(event.submissionId()).isEqualTo(55L);
        assertThat(event.quizId()).isEqualTo(QUIZ_ID);
        assertThat(event.memberId()).isEqualTo(MEMBER_ID);
        assertThat(event.score()).isEqualTo(100);
        assertThat(event.answers()).hasSize(1);
    }

    @Test
    void submitRejectsWhenQuizDoesNotExist() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        SubmitQuizCommand command = new SubmitQuizCommand(QUIZ_ID, MEMBER_ID, List.of());

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
        verify(quizSubmissionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void submitRejectsWhenMemberAlreadySubmitted() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz()));
        when(quizSubmissionRepository.existsByQuizIdAndMemberId(QUIZ_ID, MEMBER_ID)).thenReturn(true);

        SubmitQuizCommand command = new SubmitQuizCommand(QUIZ_ID, MEMBER_ID,
                List.of(new SubmitQuizCommand.AnswerCommand(10L, 102L)));

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ALREADY_SUBMITTED);
        verify(quizSubmissionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
