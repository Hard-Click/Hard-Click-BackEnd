package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.AdminCourseQuizzes;
import com.wanted.backend.domain.quiz.application.result.AdminQuizCourse;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.usecase.AdminQuizCourseQueryUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.presentation.request.InstructorQuizRequest;
import com.wanted.backend.domain.quiz.presentation.response.AdminCourseQuizListResponse;
import com.wanted.backend.domain.quiz.presentation.response.AdminQuizCourseListResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDeleteResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDetailResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizMutationResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizStatisticsResponse;
import com.wanted.backend.global.common.ApiResponse;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminQuizControllerTest {

    private QuizQueryUseCase quizQueryUseCase;
    private QuizCommandUseCase quizCommandUseCase;
    private AdminQuizCourseQueryUseCase adminQuizCourseQueryUseCase;
    private AdminQuizController controller;

    @BeforeEach
    void setUp() {
        quizQueryUseCase = mock(QuizQueryUseCase.class);
        quizCommandUseCase = mock(QuizCommandUseCase.class);
        adminQuizCourseQueryUseCase = mock(AdminQuizCourseQueryUseCase.class);
        controller = new AdminQuizController(quizQueryUseCase, quizCommandUseCase, adminQuizCourseQueryUseCase);
    }

    @Test
    void getQuizMapsTheDetailFromUseCaseIncludingCorrectOptionAndExplanation() {
        when(quizQueryUseCase.getDetailByAdmin(90L)).thenReturn(new InstructorQuizDetail(
                90L, "React 기초 개념 퀴즈", 10L, "React 완벽 가이드", 100L, "섹션 1: React 기초", 1,
                LocalDateTime.of(2026, 5, 10, 15, 30),
                List.of(new InstructorQuizDetail.QuestionDetail(
                        5L, 1, "React의 가상 DOM이란?", 7L, "가상 DOM 설명", 2,
                        List.of(
                                new InstructorQuizDetail.OptionDetail(6L, 1, "실제 DOM의 복사본", false),
                                new InstructorQuizDetail.OptionDetail(7L, 2, "메모리에 존재하는 DOM의 표현", true))))
        ));

        ResponseEntity<ApiResponse<InstructorQuizDetailResponse>> result = controller.getQuiz(90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        InstructorQuizDetailResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(response.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(response.questionCount()).isEqualTo(1);
        InstructorQuizDetailResponse.Question question = response.questions().get(0);
        assertThat(question.correctOptionId()).isEqualTo(7L);
        assertThat(question.explanation()).isEqualTo("가상 DOM 설명");
        assertThat(question.difficulty()).isEqualTo(2);
        assertThat(question.options()).hasSize(2);
        assertThat(question.options().get(1).correct()).isTrue();
        verify(quizQueryUseCase).getDetailByAdmin(90L);
    }

    @Test
    void deleteQuizDelegatesToUseCaseAndReturnsDeletedStatus() {
        ResponseEntity<ApiResponse<InstructorQuizDeleteResponse>> result = controller.deleteQuiz(90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        InstructorQuizDeleteResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.status()).isEqualTo("DELETED");
        verify(quizCommandUseCase).deleteByAdmin(90L);
    }

    @Test
    void createQuizDelegatesToAdminUseCaseAndMapsTheRequest() {
        InstructorQuizRequest request = new InstructorQuizRequest(
                "관리자 등록 퀴즈", 10L, 100L,
                List.of(new InstructorQuizRequest.Question("문제 내용", "해설", 1, 2,
                        List.of(
                                new InstructorQuizRequest.Option("보기1"),
                                new InstructorQuizRequest.Option("보기2"),
                                new InstructorQuizRequest.Option("보기3"),
                                new InstructorQuizRequest.Option("보기4")))));
        when(quizCommandUseCase.createByAdmin(any(CreateAdminQuizCommand.class))).thenReturn(77L);

        ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> result = controller.createQuiz(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        InstructorQuizMutationResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(77L);
        assertThat(response.quizTitle()).isEqualTo("관리자 등록 퀴즈");
        assertThat(response.questionCount()).isEqualTo(1);

        ArgumentCaptor<CreateAdminQuizCommand> captor = ArgumentCaptor.forClass(CreateAdminQuizCommand.class);
        verify(quizCommandUseCase).createByAdmin(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(10L);
        assertThat(captor.getValue().sectionId()).isEqualTo(100L);
        assertThat(captor.getValue().questions()).hasSize(1);
        assertThat(captor.getValue().questions().get(0).correctOptionNumber()).isEqualTo(2);
        assertThat(captor.getValue().questions().get(0).difficulty()).isEqualTo(1);
    }

    @Test
    void updateQuizDelegatesToAdminUseCaseAndMapsTheRequest() {
        InstructorQuizRequest request = new InstructorQuizRequest(
                "수정된 퀴즈", 10L, 100L,
                List.of(new InstructorQuizRequest.Question("문제 내용", "해설", 2, 3,
                        List.of(
                                new InstructorQuizRequest.Option("보기1"),
                                new InstructorQuizRequest.Option("보기2"),
                                new InstructorQuizRequest.Option("보기3"),
                                new InstructorQuizRequest.Option("보기4")))));
        when(quizCommandUseCase.updateByAdmin(any(UpdateAdminQuizCommand.class))).thenReturn(90L);

        ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> result = controller.updateQuiz(90L, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        InstructorQuizMutationResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.quizTitle()).isEqualTo("수정된 퀴즈");
        assertThat(response.questionCount()).isEqualTo(1);

        ArgumentCaptor<UpdateAdminQuizCommand> captor = ArgumentCaptor.forClass(UpdateAdminQuizCommand.class);
        verify(quizCommandUseCase).updateByAdmin(captor.capture());
        assertThat(captor.getValue().quizId()).isEqualTo(90L);
        assertThat(captor.getValue().courseId()).isEqualTo(10L);
        assertThat(captor.getValue().sectionId()).isEqualTo(100L);
        assertThat(captor.getValue().questions().get(0).correctOptionNumber()).isEqualTo(3);
    }

    @Test
    void getQuizStatisticsMapsResultAndBuildsQueryWithoutInstructor() {
        when(quizQueryUseCase.getStatisticsByAdmin(any(QuizStatisticsQuery.class))).thenReturn(
                new InstructorQuizStatistics("React 완벽 가이드", 100L, "1주차", 1, "React 기초 개념 퀴즈",
                        new InstructorQuizStatistics.Summary(3, 2, 1, 75),
                        List.of(new InstructorQuizStatistics.ScoreDistribution("90~100", 1, 50)),
                        List.of(new InstructorQuizStatistics.StudentScore("choiaa", "최아", true, 90,
                                LocalDateTime.of(2026, 5, 10, 0, 0)))));

        ResponseEntity<ApiResponse<InstructorQuizStatisticsResponse>> result =
                controller.getQuizStatistics(90L, null, null, null, null, null);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        InstructorQuizStatisticsResponse response = result.getBody().data();
        assertThat(response.sectionId()).isEqualTo(100L);
        assertThat(response.weekNumber()).isEqualTo(1);
        assertThat(response.summary().totalCount()).isEqualTo(3);
        assertThat(response.summary().averageScore()).isEqualTo(75);
        assertThat(response.students()).hasSize(1);
        assertThat(response.students().get(0).userId()).isEqualTo("choiaa");

        // 관리자 조회는 소유권 무관 → instructorId=null 로 쿼리를 만든다
        ArgumentCaptor<QuizStatisticsQuery> captor = ArgumentCaptor.forClass(QuizStatisticsQuery.class);
        verify(quizQueryUseCase).getStatisticsByAdmin(captor.capture());
        assertThat(captor.getValue().instructorId()).isNull();
        assertThat(captor.getValue().quizId()).isEqualTo(90L);
    }

    @Test
    void getQuizCoursesMapsTheResultAndPassesFilters() {
        when(adminQuizCourseQueryUseCase.getCourses("수학1", 5L, null, "React")).thenReturn(List.of(
                new AdminQuizCourse(1L, "React 완벽 가이드", true, 89, "안현",
                        Instant.parse("2026-05-10T00:00:00Z"))));

        ResponseEntity<ApiResponse<AdminQuizCourseListResponse>> result =
                controller.getQuizCourses("수학1", 5L, null, "React");

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        AdminQuizCourseListResponse response = result.getBody().data();
        assertThat(response.courses()).hasSize(1);
        AdminQuizCourseListResponse.AdminQuizCourseItem item = response.courses().get(0);
        assertThat(item.courseId()).isEqualTo(1L);
        assertThat(item.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(item.visible()).isTrue();
        assertThat(item.studentCount()).isEqualTo(89);
        assertThat(item.instructorName()).isEqualTo("안현");
        assertThat(item.registeredAt()).isNotNull();
        verify(adminQuizCourseQueryUseCase).getCourses("수학1", 5L, null, "React");
    }

    @Test
    void getQuizCoursesReturnsEmptyListWhenNoMatch() {
        when(adminQuizCourseQueryUseCase.getCourses(null, null, null, null)).thenReturn(List.of());

        ResponseEntity<ApiResponse<AdminQuizCourseListResponse>> result =
                controller.getQuizCourses(null, null, null, null);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().data().courses()).isEmpty();
        verify(adminQuizCourseQueryUseCase).getCourses(null, null, null, null);
    }

    @Test
    void getQuizCoursesMapsMultipleItemsIncludingFallbacks() {
        when(adminQuizCourseQueryUseCase.getCourses(null, null, null, null)).thenReturn(List.of(
                new AdminQuizCourse(1L, "React 기초", true, 10, "김강사", Instant.parse("2026-05-10T00:00:00Z")),
                new AdminQuizCourse(2L, "Vue 기초", false, 0, "알 수 없음", Instant.parse("2026-05-11T00:00:00Z"))));

        ResponseEntity<ApiResponse<AdminQuizCourseListResponse>> result =
                controller.getQuizCourses(null, null, null, null);

        assertThat(result.getBody().data().courses()).hasSize(2);
        AdminQuizCourseListResponse.AdminQuizCourseItem second = result.getBody().data().courses().get(1);
        assertThat(second.visible()).isFalse();
        assertThat(second.studentCount()).isZero();
        assertThat(second.instructorName()).isEqualTo("알 수 없음");
    }

    @Test
    void getCourseQuizzesMapsWeeklyQuizzesWithStatusAndExamDate() {
        when(quizQueryUseCase.getCourseQuizzesByAdmin(1L, null)).thenReturn(
                new AdminCourseQuizzes(1L, "React 완벽 가이드", List.of(
                        new AdminCourseQuizzes.WeeklyQuiz(90L, 1, "1주차 퀴즈", 10,
                                LocalDateTime.of(2026, 5, 12, 0, 0)))));

        ResponseEntity<ApiResponse<AdminCourseQuizListResponse>> result = controller.getCourseQuizzes(1L, null);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        AdminCourseQuizListResponse response = result.getBody().data();
        assertThat(response.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(response.weeks()).hasSize(1);
        AdminCourseQuizListResponse.WeeklyQuiz week = response.weeks().get(0);
        assertThat(week.quizId()).isEqualTo(90L);
        assertThat(week.weekNumber()).isEqualTo(1);
        assertThat(week.status()).isEqualTo("완료");           // 등록된 퀴즈는 항상 완료
        assertThat(week.totalQuestionCount()).isEqualTo(10);
        assertThat(week.examDate()).isNotNull();               // createdAt → examDate
        verify(quizQueryUseCase).getCourseQuizzesByAdmin(1L, null);
    }
}
