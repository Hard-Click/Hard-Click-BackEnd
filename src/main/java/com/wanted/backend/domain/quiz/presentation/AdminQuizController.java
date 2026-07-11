package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.presentation.request.InstructorQuizRequest;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDeleteResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizDetailResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizMutationResponse;
import com.wanted.backend.domain.quiz.presentation.response.InstructorQuizStatisticsResponse;
import com.wanted.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/quizzes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Quiz", description = "관리자 퀴즈 관리 API")
public class AdminQuizController {

    private final QuizQueryUseCase quizQueryUseCase;
    private final QuizCommandUseCase quizCommandUseCase;

    @GetMapping("/{quizId}")
    @Operation(summary = "퀴즈 상세 조회", description = "수정 모달에서 기존 문항/정답/해설을 로드하기 위한 퀴즈 상세를 조회합니다. ADMIN 권한 필요.")
    public ResponseEntity<ApiResponse<InstructorQuizDetailResponse>> getQuiz(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId
    ) {
        // 관리자는 소유권 검증 없이 조회 (인가는 @PreAuthorize("hasRole('ADMIN')")가 보장).
        InstructorQuizDetail detail = quizQueryUseCase.getQuizDetail(quizId);

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

        return ApiResponse.success("퀴즈 상세 정보를 조회했습니다.", response);
    }

    @GetMapping("/courses")
    @Operation(summary = "퀴즈 관리 강의 목록 조회", description = "과목/강사/강의/검색어로 필터링하여 퀴즈를 관리할 강의 목록을 조회합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<AdminQuizCourseListResponse>> getQuizCourses(
            @Parameter(description = "과목명", example = "수학1") @RequestParam(required = false) String subject,
            @Parameter(description = "강사 ID", example = "1") @RequestParam(required = false) Long instructorId,
            @Parameter(description = "강의 ID", example = "1") @RequestParam(required = false) Long courseId,
            @Parameter(description = "강의 검색어", example = "React") @RequestParam(required = false) String keyword
    ) {
        AdminQuizCourseListResponse response = new AdminQuizCourseListResponse(
                List.of(
                        new AdminQuizCourseListResponse.AdminQuizCourseItem(
                                1L, "React 완벽 가이드", true, 89, "안현", OffsetDateTime.parse("2026-05-10T00:00:00+09:00")
                        ),
                        new AdminQuizCourseListResponse.AdminQuizCourseItem(
                                2L, "수1 정복하기", false, 124, "김종호", OffsetDateTime.parse("2026-04-22T00:00:00+09:00")
                        )
                )
        );

        return ApiResponse.success("관리자 퀴즈 관리 강의 목록을 조회했습니다.", response);
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "강의별 주차 퀴즈 목록 조회", description = "특정 강의의 주차(섹션)별 퀴즈 목록을 조회합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<AdminCourseQuizListResponse>> getCourseQuizzes(
            @Parameter(description = "강의 ID", example = "1") @PathVariable Long courseId,
            @Parameter(description = "섹션(주차) ID", example = "1") @RequestParam(required = false) Long sectionId
    ) {
        AdminCourseQuizListResponse response = new AdminCourseQuizListResponse(
                courseId,
                "React 완벽 가이드",
                List.of(
                        new AdminCourseQuizListResponse.WeeklyQuiz(90L, 1, "React 기초 개념", "완료", 10, OffsetDateTime.parse("2026-05-12T00:00:00+09:00")),
                        new AdminCourseQuizListResponse.WeeklyQuiz(91L, 2, "React 기초 개념", "완료", 10, OffsetDateTime.parse("2026-05-12T00:00:00+09:00")),
                        new AdminCourseQuizListResponse.WeeklyQuiz(92L, 3, "React 기초 개념", "완료", 10, OffsetDateTime.parse("2026-05-12T00:00:00+09:00")),
                        new AdminCourseQuizListResponse.WeeklyQuiz(93L, 4, "React 기초 개념", "완료", 10, OffsetDateTime.parse("2026-05-12T00:00:00+09:00"))
                )
        );

        return ApiResponse.success("강의의 주차별 퀴즈 목록을 조회했습니다.", response);
    }

    @PostMapping
    @Operation(summary = "퀴즈 등록", description = "강의/섹션에 연결된 퀴즈를 등록합니다. instructor_id는 대상 강의의 주인에게 자동 귀속됩니다. ADMIN 권한 필요.")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> createQuiz(
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        // 관리자는 소유권 검증 없이 등록하되, 퀴즈는 강의 주인에게 귀속된다 (인가는 @PreAuthorize가 보장).
        Long quizId = quizCommandUseCase.createByAdmin(new CreateAdminQuizCommand(
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        ));

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.created("퀴즈가 등록되었습니다.", response);
    }

    private List<QuizQuestionCommand> toQuestionCommands(List<InstructorQuizRequest.Question> questions) {
        return questions.stream()
                .map(q -> new QuizQuestionCommand(
                        q.questionText(),
                        q.explanation(),
                        q.correctOptionNumber(),
                        q.options().stream().map(InstructorQuizRequest.Option::optionText).toList()))
                .toList();
    }

    @PutMapping("/{quizId}")
    @Operation(summary = "퀴즈 수정", description = "등록된 퀴즈의 제목/문항을 수정합니다. ADMIN 권한 필요.")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> updateQuiz(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId,
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        // 관리자는 소유권 검증 없이 수정 (인가는 @PreAuthorize("hasRole('ADMIN')")가 보장).
        quizCommandUseCase.updateByAdmin(new UpdateAdminQuizCommand(
                quizId,
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        ));

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.success("퀴즈가 수정되었습니다.", response);
    }

    @DeleteMapping("/{quizId}")
    @Operation(summary = "퀴즈 삭제", description = "등록된 퀴즈를 삭제(soft-delete)합니다. 학생 제출 이력은 보존됩니다. ADMIN 권한 필요.")
    public ResponseEntity<ApiResponse<InstructorQuizDeleteResponse>> deleteQuiz(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId
    ) {
        // 관리자는 소유권 검증 없이 삭제 (인가는 @PreAuthorize("hasRole('ADMIN')")가 보장).
        quizCommandUseCase.deleteQuiz(quizId);

        InstructorQuizDeleteResponse response = new InstructorQuizDeleteResponse(
                quizId, "DELETED", OffsetDateTime.now());

        return ApiResponse.success("퀴즈가 삭제되었습니다.", response);
    }

    @GetMapping("/{quizId}/statistics")
    @Operation(summary = "퀴즈 점수 현황 조회", description = "퀴즈의 응시 현황, 점수 분포, 수강생별 점수를 조회합니다. ADMIN 권한 필요.")
    public ResponseEntity<ApiResponse<InstructorQuizStatisticsResponse>> getQuizStatistics(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        // 관리자는 소유권 검증 없이 조회 (instructorId=null). 인가는 @PreAuthorize("hasRole('ADMIN')")가 보장.
        QuizStatisticsQuery query = QuizStatisticsQuery.of(null, quizId, keyword, sort, filter, page, size);
        InstructorQuizStatistics statistics = quizQueryUseCase.getQuizStatistics(query);

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

    @Schema(description = "퀴즈 관리 강의 목록")
    public record AdminQuizCourseListResponse(List<AdminQuizCourseItem> courses) {
        @Schema(description = "퀴즈 관리 강의 항목")
        public record AdminQuizCourseItem(
                @Schema(description = "강의 ID", example = "1") Long courseId,
                @Schema(description = "강의명", example = "React 완벽 가이드") String courseTitle,
                @Schema(description = "공개 여부", example = "true") boolean visible,
                @Schema(description = "수강생 수", example = "89") int studentCount,
                @Schema(description = "강사명", example = "안현") String instructorName,
                @Schema(description = "강의 등록일시") OffsetDateTime registeredAt
        ) {
        }
    }

    @Schema(description = "강의별 주차 퀴즈 목록")
    public record AdminCourseQuizListResponse(
            @Schema(description = "강의 ID", example = "1") Long courseId,
            @Schema(description = "강의명", example = "React 완벽 가이드") String courseTitle,
            @Schema(description = "주차별 퀴즈 목록") List<WeeklyQuiz> weeks
    ) {
        @Schema(description = "주차별 퀴즈")
        public record WeeklyQuiz(
                @Schema(description = "퀴즈 ID", example = "90") Long quizId,
                @Schema(description = "주차", example = "1") int weekNumber,
                @Schema(description = "퀴즈명", example = "React 기초 개념") String quizTitle,
                @Schema(description = "등록 상태", example = "완료") String status,
                @Schema(description = "총 문제 수", example = "10") int totalQuestionCount,
                @Schema(description = "응시일") OffsetDateTime examDate
        ) {
        }
    }

}
