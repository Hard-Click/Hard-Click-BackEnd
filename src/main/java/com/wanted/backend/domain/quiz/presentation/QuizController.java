package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.SubmitQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.result.QuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.result.StudentQuizDetail;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizSubmissionUseCase;
import com.wanted.backend.domain.quiz.presentation.request.InstructorQuizRequest;
import com.wanted.backend.domain.quiz.presentation.request.QuizSubmissionRequest;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDeleteResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDetailResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizListResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizMutationResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizStatisticsResponse;
import com.wanted.backend.domain.quiz.presentation.response.MyQuizListResponse;
import com.wanted.backend.domain.quiz.presentation.response.QuizReportResponse;
import com.wanted.backend.domain.quiz.presentation.response.QuizSubmissionResponse;
import com.wanted.backend.domain.quiz.presentation.response.StudentQuizDetailResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Quiz", description = "학생/강사 퀴즈 API")
public class QuizController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final QuizCommandUseCase quizCommandUseCase;
    private final QuizQueryUseCase quizQueryUseCase;
    private final QuizSubmissionUseCase quizSubmissionUseCase;

    @GetMapping("/members/me/quizzes")
    @Operation(summary = "내 퀴즈 목록 조회", description = "선택한 강의의 주차별 퀴즈와 내 응시 현황(완료 수/평균 점수)을 조회합니다.")
    public ResponseEntity<ApiResponse<MyQuizListResponse>> getMyQuizzes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long courseId
    ) {
        MyQuizList myQuizzes = quizQueryUseCase.getMyQuizzes(userDetails.getMemberId(), courseId);

        MyQuizListResponse response = new MyQuizListResponse(
                myQuizzes.courseId(),
                myQuizzes.courseTitle(),
                new MyQuizListResponse.Summary(myQuizzes.completedCount(), myQuizzes.averageScore()),
                myQuizzes.quizzes().stream()
                        .map(item -> new MyQuizListResponse.MyQuizItem(
                                item.quizId(),
                                item.weekNumber(),
                                item.quizTitle(),
                                item.questionCount(),
                                item.completed(),
                                item.score(),
                                item.submittedAt() == null ? null
                                        : item.submittedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                        .toList()
        );

        return ApiResponse.success("내 퀴즈 목록을 조회했습니다.", response);
    }

    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "학생 퀴즈 상세 조회", description = "학생이 풀이할 퀴즈 상세 정보와 문항 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<StudentQuizDetailResponse>> getStudentQuizDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        StudentQuizDetail detail = quizQueryUseCase.getStudentQuizDetail(userDetails.getMemberId(), quizId);

        StudentQuizDetailResponse response = new StudentQuizDetailResponse(
                detail.quizId(),
                detail.quizTitle(),
                detail.courseTitle(),
                detail.sectionTitle(),
                detail.totalQuestionCount(),
                detail.answeredCount(),
                detail.submitted(),
                detail.questions().stream()
                        .map(question -> new StudentQuizDetailResponse.Question(
                                question.questionId(),
                                question.questionNumber(),
                                question.questionText(),
                                question.options().stream()
                                        .map(option -> new StudentQuizDetailResponse.Option(
                                                option.optionId(),
                                                option.optionNumber(),
                                                option.optionText()))
                                        .toList()))
                        .toList()
        );

        return ApiResponse.success("퀴즈 상세 정보를 조회했습니다.", response);
    }

    @PostMapping("/quizzes/{quizId}/submissions")
    @Operation(summary = "퀴즈 답안 제출", description = "학생의 퀴즈 답안을 제출하고 자동 채점 결과를 반환합니다.")
    public ResponseEntity<ApiResponse<QuizSubmissionResponse>> submitQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @RequestBody(required = false) QuizSubmissionRequest request
    ) {
        List<SubmitQuizCommand.AnswerCommand> answers =
                request == null || request.answers() == null ? List.of()
                        : request.answers().stream()
                                .map(a -> new SubmitQuizCommand.AnswerCommand(a.questionId(), a.selectedOptionId()))
                                .toList();

        QuizSubmissionResult result = quizSubmissionUseCase.submit(
                new SubmitQuizCommand(quizId, userDetails.getMemberId(), answers));

        QuizSubmissionResponse response = new QuizSubmissionResponse(
                result.submissionId(),
                result.quizId(),
                result.score(),
                result.totalQuestionCount(),
                result.correctCount(),
                result.incorrectCount(),
                result.submittedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );

        return ApiResponse.success("퀴즈가 제출되었습니다.", response);
    }

    @GetMapping("/quizzes/{quizId}/reports/me")
    @Operation(summary = "내 퀴즈 리포트 조회", description = "현재 로그인한 회원의 퀴즈 제출 결과와 해설 리포트를 조회합니다.")
    public ResponseEntity<ApiResponse<QuizReportResponse>> getMyQuizReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        QuizReport report = quizQueryUseCase.getMyQuizReport(userDetails.getMemberId(), quizId);

        QuizReportResponse response = new QuizReportResponse(
                report.quizId(),
                report.week(),
                report.quizTitle(),
                report.submittedAt() == null ? null
                        : report.submittedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                report.score(),
                report.totalScore(),
                report.correctCount(),
                report.incorrectCount(),
                report.scoreDiff(),
                report.previousScore(),
                report.wrongNotes().stream().map(QuizController::toQuestionResultResponse).toList(),
                report.questions().stream().map(QuizController::toQuestionResultResponse).toList()
        );

        return ApiResponse.success("오답노트 및 리포트를 조회했습니다.", response);
    }

    private static QuizReportResponse.QuestionResult toQuestionResultResponse(QuizReport.QuestionResult q) {
        return new QuizReportResponse.QuestionResult(
                q.questionId(),
                q.questionNumber(),
                q.questionText(),
                q.correctOptionId(),
                q.selectedOptionId(),
                q.correct(),
                q.explanation(),
                q.options().stream()
                        .map(o -> new QuizReportResponse.Option(o.optionId(), o.optionNumber(), o.optionText()))
                        .toList());
    }

    @GetMapping("/instructor/quizzes")
    @Operation(summary = "강사 퀴즈 목록 조회", description = "강사가 관리하는 퀴즈 목록을 강의/섹션 기준으로 조회합니다")
    public ResponseEntity<ApiResponse<InstructorQuizListResponse>> getInstructorQuizzes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long sectionId
    ) {
        List<InstructorQuizSummary> summaries = quizQueryUseCase
                .getInstructorQuizzes(userDetails.getMemberId(), courseId, sectionId);

        InstructorQuizListResponse response = new InstructorQuizListResponse(
                courseId,
                sectionId,
                summaries.stream()
                        .map(summary -> new InstructorQuizListResponse.InstructorQuizItem(
                                summary.quizId(),
                                summary.quizTitle(),
                                summary.courseTitle(),
                                summary.sectionId(),
                                summary.weekNumber(),
                                summary.sectionTitle(),
                                summary.questionCount(),
                                summary.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                        .toList()
        );

        return ApiResponse.success("강사 퀴즈 목록을 조회했습니다.", response);
    }

    @GetMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 상세 조회", description = "강사가 관리하는 퀴즈 상세 정보와 정답 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizDetailResponse>> getInstructorQuizDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        InstructorQuizDetail detail = quizQueryUseCase
                .getInstructorQuizDetail(userDetails.getMemberId(), quizId);

        InstructorQuizDetailResponse response = new InstructorQuizDetailResponse(
                detail.quizId(),
                detail.quizTitle(),
                detail.courseId(),
                detail.courseTitle(),
                detail.sectionId(),
                detail.sectionTitle(),
                detail.questionCount(),
                detail.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                detail.questions().stream()
                        .map(question -> new InstructorQuizDetailResponse.Question(
                                question.questionId(),
                                question.questionNumber(),
                                question.questionText(),
                                question.correctOptionId(),
                                question.explanation(),
                                question.options().stream()
                                        .map(option -> new InstructorQuizDetailResponse.Option(
                                                option.optionId(),
                                                option.optionNumber(),
                                                option.optionText(),
                                                option.correct()))
                                        .toList()))
                        .toList()
        );

        return ApiResponse.success("강사 퀴즈 상세 정보를 조회했습니다.", response);
    }

    @PostMapping("/instructor/quizzes")
    @Operation(summary = "강사 퀴즈 등록", description = "강사가 강의/섹션에 연결된 퀴즈를 등록합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> createInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        CreateQuizCommand command = new CreateQuizCommand(
                userDetails.getMemberId(),
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        );

        Long quizId = quizCommandUseCase.create(command);

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.created("퀴즈가 등록되었습니다.", response);
    }

    @PutMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 수정", description = "강사가 등록한 퀴즈의 제목과 문항 정보를 수정합니다")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> updateInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        UpdateQuizCommand command = new UpdateQuizCommand(
                quizId,
                userDetails.getMemberId(),
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        );

        quizCommandUseCase.update(command);

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.success("퀴즈가 수정되었습니다.", response);
    }

    @DeleteMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 삭제", description = "강사가 등록한 퀴즈를 삭제합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizDeleteResponse>> deleteInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        quizCommandUseCase.delete(new DeleteQuizCommand(quizId, userDetails.getMemberId()));

        InstructorQuizDeleteResponse response = new InstructorQuizDeleteResponse(
                quizId,
                "DELETED",
                OffsetDateTime.now()
        );

        return ApiResponse.success("퀴즈가 삭제되었습니다.", response);
    }

    @GetMapping("/instructors/me/quizzes/{quizId}/statistics")
    @Operation(summary = "강사 퀴즈 통계 조회", description = "강사가 관리하는 퀴즈의 응시 현황과 점수 분포를 조회합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizStatisticsResponse>> getInstructorQuizStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        QuizStatisticsQuery query = new QuizStatisticsQuery(
                userDetails.getMemberId(),
                quizId,
                keyword,
                parseSort(sort),
                parseFilter(filter),
                normalizePage(page),
                normalizeSize(size)
        );

        InstructorQuizStatistics statistics = quizQueryUseCase.getInstructorQuizStatistics(query);

        InstructorQuizStatisticsResponse response = new InstructorQuizStatisticsResponse(
                statistics.courseTitle(),
                statistics.sectionTitle(),
                statistics.week(),
                statistics.quizTitle(),
                new InstructorQuizStatisticsResponse.Summary(
                        statistics.summary().totalCount(),
                        statistics.summary().submittedCount(),
                        statistics.summary().notSubmittedCount(),
                        statistics.summary().averageScore()),
                statistics.scoreDistribution().stream()
                        .map(d -> new InstructorQuizStatisticsResponse.ScoreDistribution(
                                d.range(), d.count(), d.percentage()))
                        .toList(),
                statistics.students().stream()
                        .map(s -> new InstructorQuizStatisticsResponse.StudentScore(
                                s.userId(),
                                s.name(),
                                s.submitted(),
                                s.score(),
                                s.submittedAt() == null ? null
                                        : s.submittedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                        .toList()
        );

        return ApiResponse.success("퀴즈 점수 현황을 조회했습니다.", response);
    }

    private QuizStatisticsQuery.SortType parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return QuizStatisticsQuery.SortType.SCORE_DESC;
        }
        try {
            return QuizStatisticsQuery.SortType.valueOf(sort.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return QuizStatisticsQuery.SortType.SCORE_DESC;
        }
    }

    private QuizStatisticsQuery.FilterType parseFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return QuizStatisticsQuery.FilterType.ALL;
        }
        try {
            return QuizStatisticsQuery.FilterType.valueOf(filter.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return QuizStatisticsQuery.FilterType.ALL;
        }
    }

    private List<QuizQuestionCommand> toQuestionCommands(List<InstructorQuizRequest.Question> questions) {
        return questions.stream()
                .map(q -> new QuizQuestionCommand(
                        q.questionText(),
                        q.explanation(),
                        q.correctOptionNumber(),
                        q.options().stream().map(InstructorQuizRequest.Option::optionText).toList()
                ))
                .toList();
    }

    private int normalizePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        long offset = (long) page * size;
        if (offset >= items.size()) {
            return List.of();
        }

        int fromIndex = (int) offset;
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

}
