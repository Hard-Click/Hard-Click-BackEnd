package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
}
