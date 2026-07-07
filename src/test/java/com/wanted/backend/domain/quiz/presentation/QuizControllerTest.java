package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import com.wanted.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizControllerTest {

    private QuizCommandUseCase quizCommandUseCase;
    private QuizQueryUseCase quizQueryUseCase;
    private QuizController controller;

    @BeforeEach
    void setUp() {
        quizCommandUseCase = mock(QuizCommandUseCase.class);
        quizQueryUseCase = mock(QuizQueryUseCase.class);
        controller = new QuizController(quizCommandUseCase, quizQueryUseCase);
    }

    @Test
    void instructorQuizzesMapTheQuerySummariesToTheListResponse() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getInstructorQuizzes(1L, 10L, null)).thenReturn(List.of(
                new InstructorQuizSummary(90L, "1주차 퀴즈", 10L, "React 완벽 가이드", 100L,
                        "섹션 1: React 기초", 8, java.time.LocalDateTime.of(2026, 5, 10, 15, 30))
        ));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizListResponse>> result =
                controller.getInstructorQuizzes(userDetails, 10L, null);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.InstructorQuizListResponse response = result.getBody().data();
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.quizzes()).hasSize(1);
        QuizController.InstructorQuizListResponse.InstructorQuizItem item = response.quizzes().get(0);
        assertThat(item.quizId()).isEqualTo(90L);
        assertThat(item.quizTitle()).isEqualTo("1주차 퀴즈");
        assertThat(item.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(item.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(item.questionCount()).isEqualTo(8);
        assertThat(item.createdAt().toLocalDateTime())
                .isEqualTo(java.time.LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void instructorQuizzesReturnAnEmptyListWhenTheInstructorHasNoQuizzes() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getInstructorQuizzes(1L, null, null)).thenReturn(List.of());

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizListResponse>> result =
                controller.getInstructorQuizzes(userDetails, null, null);

        assertThat(result.getBody().data().quizzes()).isEmpty();
    }

    @Test
    void myQuizzesIncludeCourseIdForEachItem() {
        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.MyQuizListResponse>> result =
                controller.getMyQuizzes(null, null, null);

        QuizController.MyQuizListResponse response = result.getBody().data();
        assertThat(response.quizzes()).isNotEmpty();
        assertThat(response.quizzes()).allSatisfy(item -> assertThat(item.courseId()).isNotNull());

        QuizController.MyQuizListResponse.MyQuizItem reactQuiz = response.quizzes().stream()
                .filter(item -> item.quizId().equals(90L))
                .findFirst()
                .orElseThrow();
        assertThat(reactQuiz.courseId()).isEqualTo(1L);
        assertThat(reactQuiz.courseTitle()).isEqualTo("React 완벽 가이드");
    }

    private QuizController.InstructorQuizRequest quizRequest() {
        return new QuizController.InstructorQuizRequest(
                "React 기초 개념 퀴즈", 10L, 100L,
                List.of(new QuizController.InstructorQuizRequest.Question(
                        "React의 가상 DOM이란?", "설명", 2,
                        List.of(
                                new QuizController.InstructorQuizRequest.Option("보기1"),
                                new QuizController.InstructorQuizRequest.Option("보기2"),
                                new QuizController.InstructorQuizRequest.Option("보기3"),
                                new QuizController.InstructorQuizRequest.Option("보기4")
                        )))
        );
    }

    @Test
    void createInstructorQuizMapsTheRequestToACommandAndReturnsTheCreatedQuiz() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizCommandUseCase.create(org.mockito.ArgumentMatchers.any())).thenReturn(999L);

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizMutationResponse>> result =
                controller.createInstructorQuiz(userDetails, quizRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        QuizController.InstructorQuizMutationResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(999L);
        assertThat(response.quizTitle()).isEqualTo("React 기초 개념 퀴즈");
        assertThat(response.questionCount()).isEqualTo(1);

        ArgumentCaptor<CreateQuizCommand> captor = ArgumentCaptor.forClass(CreateQuizCommand.class);
        verify(quizCommandUseCase).create(captor.capture());
        CreateQuizCommand command = captor.getValue();
        assertThat(command.instructorId()).isEqualTo(1L);
        assertThat(command.courseId()).isEqualTo(10L);
        assertThat(command.sectionId()).isEqualTo(100L);
        assertThat(command.quizTitle()).isEqualTo("React 기초 개념 퀴즈");
        assertThat(command.questions()).hasSize(1);
        QuizQuestionCommand question = command.questions().get(0);
        assertThat(question.questionText()).isEqualTo("React의 가상 DOM이란?");
        assertThat(question.correctOptionNumber()).isEqualTo(2);
        assertThat(question.optionTexts()).containsExactly("보기1", "보기2", "보기3", "보기4");
    }

    @Test
    void updateInstructorQuizMapsTheRequestToACommandAndReturnsTheUpdatedQuiz() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizCommandUseCase.update(org.mockito.ArgumentMatchers.any())).thenReturn(90L);

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizMutationResponse>> result =
                controller.updateInstructorQuiz(userDetails, 90L, quizRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.InstructorQuizMutationResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.quizTitle()).isEqualTo("React 기초 개념 퀴즈");
        assertThat(response.questionCount()).isEqualTo(1);

        ArgumentCaptor<UpdateQuizCommand> captor = ArgumentCaptor.forClass(UpdateQuizCommand.class);
        verify(quizCommandUseCase).update(captor.capture());
        UpdateQuizCommand command = captor.getValue();
        assertThat(command.quizId()).isEqualTo(90L);
        assertThat(command.instructorId()).isEqualTo(1L);
        assertThat(command.courseId()).isEqualTo(10L);
        assertThat(command.sectionId()).isEqualTo(100L);
        assertThat(command.quizTitle()).isEqualTo("React 기초 개념 퀴즈");
        QuizQuestionCommand question = command.questions().get(0);
        assertThat(question.questionText()).isEqualTo("React의 가상 DOM이란?");
        assertThat(question.optionTexts()).containsExactly("보기1", "보기2", "보기3", "보기4");
    }

    @Test
    void deleteInstructorQuizDelegatesToTheUseCaseAndReturnsTheDeletedStatus() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizDeleteResponse>> result =
                controller.deleteInstructorQuiz(userDetails, 90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.InstructorQuizDeleteResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.status()).isEqualTo("DELETED");

        ArgumentCaptor<DeleteQuizCommand> captor = ArgumentCaptor.forClass(DeleteQuizCommand.class);
        verify(quizCommandUseCase).delete(captor.capture());
        assertThat(captor.getValue().quizId()).isEqualTo(90L);
        assertThat(captor.getValue().instructorId()).isEqualTo(1L);
    }

    @Test
    void deleteInstructorQuizPropagatesTheUseCasesBusinessExceptionWhenNotTheQuizOwner() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(999L);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED))
                .when(quizCommandUseCase).delete(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> controller.deleteInstructorQuiz(userDetails, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void updateInstructorQuizPropagatesTheUseCasesBusinessExceptionWhenNotTheQuizOwner() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(999L);
        when(quizCommandUseCase.update(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED));

        assertThatThrownBy(() -> controller.updateInstructorQuiz(userDetails, 90L, quizRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void createInstructorQuizPropagatesTheUseCasesBusinessExceptionWhenNotTheCourseOwner() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(999L);
        when(quizCommandUseCase.create(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.COURSE_ACCESS_DENIED));

        assertThatThrownBy(() -> controller.createInstructorQuiz(userDetails, quizRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_ACCESS_DENIED);
    }
}
