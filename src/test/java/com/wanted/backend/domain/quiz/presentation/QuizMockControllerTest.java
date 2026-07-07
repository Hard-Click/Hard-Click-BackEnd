package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
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

class QuizMockControllerTest {

    private CourseTitlePort courseTitlePort;
    private QuizCommandUseCase quizCommandUseCase;
    private QuizMockController controller;

    @BeforeEach
    void setUp() {
        courseTitlePort = mock(CourseTitlePort.class);
        quizCommandUseCase = mock(QuizCommandUseCase.class);
        controller = new QuizMockController(courseTitlePort, quizCommandUseCase);
    }

    @Test
    void instructorQuizzesUseTheRequestedCourseRealTitleInsteadOfAHardcodedUnrelatedTopic() {
        when(courseTitlePort.findTitleByCourseId(17L)).thenReturn(Optional.of("왕초보 중국어 회화"));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizMockController.InstructorQuizListResponse>> result =
                controller.getInstructorQuizzes(null, 17L, null);

        QuizMockController.InstructorQuizListResponse response = result.getBody().data();
        assertThat(response.courseId()).isEqualTo(17L);
        assertThat(response.quizzes()).isNotEmpty();
        assertThat(response.quizzes())
                .allSatisfy(item -> {
                    assertThat(item.courseTitle()).isEqualTo("왕초보 중국어 회화");
                    assertThat(item.quizTitle()).contains("왕초보 중국어 회화");
                });
    }

    @Test
    void instructorQuizzesFallBackToAPlaceholderWhenCourseIdDoesNotExist() {
        when(courseTitlePort.findTitleByCourseId(999L)).thenReturn(Optional.empty());

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizMockController.InstructorQuizListResponse>> result =
                controller.getInstructorQuizzes(null, 999L, null);

        QuizMockController.InstructorQuizListResponse response = result.getBody().data();
        assertThat(response.quizzes())
                .allSatisfy(item -> assertThat(item.courseTitle()).isEqualTo("강의 #999"));
    }

    @Test
    void instructorQuizzesFallBackToAPlaceholderWhenCourseIdIsNull() {
        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizMockController.InstructorQuizListResponse>> result =
                controller.getInstructorQuizzes(null, null, null);

        QuizMockController.InstructorQuizListResponse response = result.getBody().data();
        assertThat(response.quizzes())
                .allSatisfy(item -> assertThat(item.courseTitle()).isEqualTo("전체 강의"));
    }

    @Test
    void myQuizzesIncludeCourseIdForEachItem() {
        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizMockController.MyQuizListResponse>> result =
                controller.getMyQuizzes(null, null, null);

        QuizMockController.MyQuizListResponse response = result.getBody().data();
        assertThat(response.quizzes()).isNotEmpty();
        assertThat(response.quizzes()).allSatisfy(item -> assertThat(item.courseId()).isNotNull());

        QuizMockController.MyQuizListResponse.MyQuizItem reactQuiz = response.quizzes().stream()
                .filter(item -> item.quizId().equals(90L))
                .findFirst()
                .orElseThrow();
        assertThat(reactQuiz.courseId()).isEqualTo(1L);
        assertThat(reactQuiz.courseTitle()).isEqualTo("React 완벽 가이드");
    }

    private QuizMockController.InstructorQuizRequest quizRequest() {
        return new QuizMockController.InstructorQuizRequest(
                "React 기초 개념 퀴즈", 10L, 100L,
                List.of(new QuizMockController.InstructorQuizRequest.Question(
                        "React의 가상 DOM이란?", "설명", 2,
                        List.of(
                                new QuizMockController.InstructorQuizRequest.Option("보기1"),
                                new QuizMockController.InstructorQuizRequest.Option("보기2"),
                                new QuizMockController.InstructorQuizRequest.Option("보기3"),
                                new QuizMockController.InstructorQuizRequest.Option("보기4")
                        )))
        );
    }

    @Test
    void createInstructorQuizMapsTheRequestToACommandAndReturnsTheCreatedQuiz() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizCommandUseCase.create(org.mockito.ArgumentMatchers.any())).thenReturn(999L);

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizMockController.InstructorQuizMutationResponse>> result =
                controller.createInstructorQuiz(userDetails, quizRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        QuizMockController.InstructorQuizMutationResponse response = result.getBody().data();
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
        CreateQuizCommand.QuestionCommand question = command.questions().get(0);
        assertThat(question.questionText()).isEqualTo("React의 가상 DOM이란?");
        assertThat(question.correctOptionNumber()).isEqualTo(2);
        assertThat(question.optionTexts()).containsExactly("보기1", "보기2", "보기3", "보기4");
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
