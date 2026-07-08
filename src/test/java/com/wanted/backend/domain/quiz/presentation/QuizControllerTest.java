package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.SubmitQuizCommand;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.result.QuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.result.StudentQuizDetail;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizSubmissionUseCase;
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
    private QuizSubmissionUseCase quizSubmissionUseCase;
    private QuizController controller;

    @BeforeEach
    void setUp() {
        quizCommandUseCase = mock(QuizCommandUseCase.class);
        quizQueryUseCase = mock(QuizQueryUseCase.class);
        quizSubmissionUseCase = mock(QuizSubmissionUseCase.class);
        controller = new QuizController(quizCommandUseCase, quizQueryUseCase, quizSubmissionUseCase);
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
    void instructorQuizDetailMapsTheQueryResultIncludingCorrectOptionInfo() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getInstructorQuizDetail(1L, 90L)).thenReturn(new InstructorQuizDetail(
                90L, "React 기초 개념 퀴즈", 10L, "React 완벽 가이드", 100L, "섹션 1: React 기초", 1,
                java.time.LocalDateTime.of(2026, 5, 10, 15, 30),
                List.of(new InstructorQuizDetail.QuestionDetail(
                        5L, 1, "React의 가상 DOM이란?", 7L, "가상 DOM 설명",
                        List.of(
                                new InstructorQuizDetail.OptionDetail(6L, 1, "실제 DOM의 복사본", false),
                                new InstructorQuizDetail.OptionDetail(7L, 2, "메모리에 존재하는 DOM의 표현", true))))
        ));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizDetailResponse>> result =
                controller.getInstructorQuizDetail(userDetails, 90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.InstructorQuizDetailResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(response.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(response.questionCount()).isEqualTo(1);
        assertThat(response.questions()).hasSize(1);
        QuizController.InstructorQuizDetailResponse.Question question = response.questions().get(0);
        assertThat(question.questionId()).isEqualTo(5L);
        assertThat(question.correctOptionId()).isEqualTo(7L);
        assertThat(question.explanation()).isEqualTo("가상 DOM 설명");
        assertThat(question.options()).hasSize(2);
        assertThat(question.options().get(1).correct()).isTrue();
    }

    @Test
    void instructorQuizDetailPropagatesTheUseCasesBusinessExceptionWhenNotTheQuizOwner() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(999L);
        when(quizQueryUseCase.getInstructorQuizDetail(999L, 90L))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED));

        assertThatThrownBy(() -> controller.getInstructorQuizDetail(userDetails, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void submitQuizMapsTheRequestToACommandAndReturnsGradingResult() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizSubmissionUseCase.submit(org.mockito.ArgumentMatchers.any())).thenReturn(
                new QuizSubmissionResult(55L, 90L, 75, 8, 6, 2,
                        java.time.LocalDateTime.of(2026, 5, 10, 15, 30)));

        QuizController.QuizSubmissionRequest request = new QuizController.QuizSubmissionRequest(
                List.of(new QuizController.QuizSubmissionRequest.Answer(11L, 22L)));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.QuizSubmissionResponse>> result =
                controller.submitQuiz(userDetails, 90L, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.QuizSubmissionResponse response = result.getBody().data();
        assertThat(response.submissionId()).isEqualTo(55L);
        assertThat(response.score()).isEqualTo(75);
        assertThat(response.correctCount()).isEqualTo(6);
        assertThat(response.incorrectCount()).isEqualTo(2);

        ArgumentCaptor<SubmitQuizCommand> captor = ArgumentCaptor.forClass(SubmitQuizCommand.class);
        verify(quizSubmissionUseCase).submit(captor.capture());
        SubmitQuizCommand command = captor.getValue();
        assertThat(command.quizId()).isEqualTo(90L);
        assertThat(command.memberId()).isEqualTo(1L);
        assertThat(command.answers()).hasSize(1);
        assertThat(command.answers().get(0).questionId()).isEqualTo(11L);
        assertThat(command.answers().get(0).selectedOptionId()).isEqualTo(22L);
    }

    @Test
    void submitQuizTreatsANullBodyAsAnEmptyAnswerList() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizSubmissionUseCase.submit(org.mockito.ArgumentMatchers.any())).thenReturn(
                new QuizSubmissionResult(55L, 90L, 0, 8, 0, 8,
                        java.time.LocalDateTime.of(2026, 5, 10, 15, 30)));

        controller.submitQuiz(userDetails, 90L, null);

        ArgumentCaptor<SubmitQuizCommand> captor = ArgumentCaptor.forClass(SubmitQuizCommand.class);
        verify(quizSubmissionUseCase).submit(captor.capture());
        assertThat(captor.getValue().answers()).isEmpty();
    }

    @Test
    void submitQuizPropagatesTheUseCasesBusinessExceptionWhenAlreadySubmitted() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizSubmissionUseCase.submit(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_ALREADY_SUBMITTED));

        QuizController.QuizSubmissionRequest request = new QuizController.QuizSubmissionRequest(
                List.of(new QuizController.QuizSubmissionRequest.Answer(11L, 22L)));

        assertThatThrownBy(() -> controller.submitQuiz(userDetails, 90L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ALREADY_SUBMITTED);
    }

    @Test
    void quizReportMapsTheQueryResultIncludingWrongNotesAndScoreDiff() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        QuizReport.QuestionResult wrong = new QuizReport.QuestionResult(
                20L, 2, "질문2", 201L, 202L, false, "해설2",
                List.of(new QuizReport.OptionView(201L, 1, "보기1"),
                        new QuizReport.OptionView(202L, 2, "보기2")));
        QuizReport.QuestionResult correctQ = new QuizReport.QuestionResult(
                10L, 1, "질문1", 102L, 102L, true, "해설1",
                List.of(new QuizReport.OptionView(101L, 1, "보기1"),
                        new QuizReport.OptionView(102L, 2, "보기2")));
        when(quizQueryUseCase.getMyQuizReport(1L, 90L)).thenReturn(new QuizReport(
                90L, 2, "2주차 퀴즈", java.time.LocalDateTime.of(2026, 5, 12, 0, 0),
                60, 100, 1, 1, -15, List.of(wrong), List.of(correctQ, wrong)));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.QuizReportResponse>> result =
                controller.getMyQuizReport(userDetails, 90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.QuizReportResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.week()).isEqualTo(2);
        assertThat(response.score()).isEqualTo(60);
        assertThat(response.totalScore()).isEqualTo(100);
        assertThat(response.scoreDiff()).isEqualTo(-15);
        assertThat(response.wrongNotes()).hasSize(1);
        assertThat(response.wrongNotes().get(0).questionId()).isEqualTo(20L);
        assertThat(response.wrongNotes().get(0).correctOptionId()).isEqualTo(201L);
        assertThat(response.questions()).hasSize(2);
    }

    @Test
    void quizReportPropagatesTheUseCasesBusinessExceptionWhenNotSubmitted() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getMyQuizReport(1L, 90L))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_SUBMISSION_NOT_FOUND));

        assertThatThrownBy(() -> controller.getMyQuizReport(userDetails, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_SUBMISSION_NOT_FOUND);
    }

    @Test
    void studentQuizDetailMapsTheQueryResultWithoutAnswerInfo() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getStudentQuizDetail(1L, 90L)).thenReturn(new StudentQuizDetail(
                90L, "React 기초 개념 퀴즈", "React 완벽 가이드", "섹션 1: React 기초", 1, 0, false,
                List.of(new StudentQuizDetail.Question(10L, 1, "React의 가상 DOM이란?",
                        List.of(
                                new StudentQuizDetail.Option(101L, 1, "보기1"),
                                new StudentQuizDetail.Option(102L, 2, "보기2"))))));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.StudentQuizDetailResponse>> result =
                controller.getStudentQuizDetail(userDetails, 90L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.StudentQuizDetailResponse response = result.getBody().data();
        assertThat(response.quizId()).isEqualTo(90L);
        assertThat(response.submitted()).isFalse();
        assertThat(response.answeredCount()).isZero();
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).options()).hasSize(2);
        assertThat(response.questions().get(0).options().get(0).optionText()).isEqualTo("보기1");
    }

    @Test
    void studentQuizDetailPropagatesTheUseCasesBusinessExceptionWhenNotEnrolled() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getStudentQuizDetail(1L, 90L))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED));

        assertThatThrownBy(() -> controller.getStudentQuizDetail(userDetails, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
    }

    @Test
    void instructorQuizStatisticsMapsQueryResultAndParsesSortFilterParams() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getInstructorQuizStatistics(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InstructorQuizStatistics(
                        "React 완벽 가이드", "1주차: React 기초", "React 기초 개념 퀴즈",
                        new InstructorQuizStatistics.Summary(6, 5, 1, 80),
                        List.of(new InstructorQuizStatistics.ScoreDistribution("90~100", 2, 40)),
                        List.of(new InstructorQuizStatistics.StudentScore(
                                "choiyea2026", "최예아", true, 100,
                                java.time.LocalDateTime.of(2026, 5, 10, 0, 0)))));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.InstructorQuizStatisticsResponse>> result =
                controller.getInstructorQuizStatistics(userDetails, 90L, "최", "SCORE_DESC", "SUBMITTED", 0, 10);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.InstructorQuizStatisticsResponse response = result.getBody().data();
        assertThat(response.summary().totalCount()).isEqualTo(6);
        assertThat(response.summary().submittedCount()).isEqualTo(5);
        assertThat(response.students().get(0).userId()).isEqualTo("choiyea2026");
        assertThat(response.students().get(0).score()).isEqualTo(100);
        assertThat(response.students().get(0).submittedAt()).isNotNull();

        // 컨트롤러가 sort/filter 문자열을 enum으로 파싱해 Query로 전달하는지 검증
        ArgumentCaptor<QuizStatisticsQuery> captor = ArgumentCaptor.forClass(QuizStatisticsQuery.class);
        verify(quizQueryUseCase).getInstructorQuizStatistics(captor.capture());
        QuizStatisticsQuery query = captor.getValue();
        assertThat(query.instructorId()).isEqualTo(1L);
        assertThat(query.quizId()).isEqualTo(90L);
        assertThat(query.keyword()).isEqualTo("최");
        assertThat(query.sort()).isEqualTo(QuizStatisticsQuery.SortType.SCORE_DESC);
        assertThat(query.filter()).isEqualTo(QuizStatisticsQuery.FilterType.SUBMITTED);
    }

    @Test
    void instructorQuizStatisticsDefaultsSortAndFilterWhenParamsInvalid() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getInstructorQuizStatistics(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InstructorQuizStatistics("c", "s", "q",
                        new InstructorQuizStatistics.Summary(0, 0, 0, 0), List.of(), List.of()));

        controller.getInstructorQuizStatistics(userDetails, 90L, null, "이상한값", null, null, null);

        ArgumentCaptor<QuizStatisticsQuery> captor = ArgumentCaptor.forClass(QuizStatisticsQuery.class);
        verify(quizQueryUseCase).getInstructorQuizStatistics(captor.capture());
        // 잘못된 sort → SCORE_DESC 기본, filter 미지정 → ALL 기본
        assertThat(captor.getValue().sort()).isEqualTo(QuizStatisticsQuery.SortType.SCORE_DESC);
        assertThat(captor.getValue().filter()).isEqualTo(QuizStatisticsQuery.FilterType.ALL);
    }

    @Test
    void instructorQuizStatisticsPropagatesBusinessExceptionWhenNotOwner() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(999L);
        when(quizQueryUseCase.getInstructorQuizStatistics(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED));

        assertThatThrownBy(() -> controller.getInstructorQuizStatistics(userDetails, 90L, null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void myQuizzesMapTheQueryResultToTheResponseWithSummaryAndWeekOrder() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getMyQuizzes(1L, 10L)).thenReturn(new MyQuizList(
                10L, "React 완벽 가이드", 2, 85,
                List.of(
                        new MyQuizList.MyQuizItem(90L, 1, "React 기초 개념", 10, true, 80,
                                java.time.LocalDateTime.of(2026, 5, 12, 0, 0)),
                        new MyQuizList.MyQuizItem(92L, 3, "State와 Lifecycle", 10, false, null, null))
        ));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.MyQuizListResponse>> result =
                controller.getMyQuizzes(userDetails, 10L);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        QuizController.MyQuizListResponse response = result.getBody().data();
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(response.summary().completedCount()).isEqualTo(2);
        assertThat(response.summary().averageScore()).isEqualTo(85);
        assertThat(response.quizzes()).hasSize(2);

        QuizController.MyQuizListResponse.MyQuizItem completed = response.quizzes().get(0);
        assertThat(completed.weekNumber()).isEqualTo(1);
        assertThat(completed.completed()).isTrue();
        assertThat(completed.score()).isEqualTo(80);
        assertThat(completed.submittedAt()).isNotNull();

        QuizController.MyQuizListResponse.MyQuizItem notSubmitted = response.quizzes().get(1);
        assertThat(notSubmitted.completed()).isFalse();
        assertThat(notSubmitted.score()).isNull();
        assertThat(notSubmitted.submittedAt()).isNull();
    }

    @Test
    void myQuizzesMapAnEmptyResultToZeroSummaryAndEmptyList() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getMyQuizzes(1L, 10L))
                .thenReturn(new MyQuizList(10L, "React 완벽 가이드", 0, 0, List.of()));

        ResponseEntity<com.wanted.backend.global.common.ApiResponse<QuizController.MyQuizListResponse>> result =
                controller.getMyQuizzes(userDetails, 10L);

        QuizController.MyQuizListResponse response = result.getBody().data();
        assertThat(response.summary().completedCount()).isZero();
        assertThat(response.summary().averageScore()).isZero();
        assertThat(response.quizzes()).isEmpty();
    }

    @Test
    void myQuizzesPropagateTheUseCasesBusinessExceptionWhenNotEnrolled() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(1L);
        when(quizQueryUseCase.getMyQuizzes(1L, 10L))
                .thenThrow(new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED));

        assertThatThrownBy(() -> controller.getMyQuizzes(userDetails, 10L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
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
