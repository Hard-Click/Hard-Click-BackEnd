package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
import java.util.List;

@RestController
@RequestMapping("/api/admin/quizzes")
@Tag(name = "Admin Quiz", description = "관리자 퀴즈 관리 API (Mock)")
public class AdminQuizMockController {

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
    @Operation(summary = "퀴즈 등록", description = "강사를 지정하여 강의/섹션에 연결된 퀴즈를 등록합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<AdminQuizMutationResponse>> createQuiz(
            @RequestBody(required = false) AdminQuizCreateRequest request
    ) {
        AdminQuizMutationResponse response = new AdminQuizMutationResponse(
                94L,
                request == null || request.quizTitle() == null ? "React 기초 개념 퀴즈" : request.quizTitle(),
                request == null || request.instructorName() == null ? "안현" : request.instructorName(),
                request == null || request.questions() == null ? 1 : request.questions().size(),
                OffsetDateTime.parse("2026-06-22T16:00:00+09:00")
        );

        return ApiResponse.created("퀴즈가 등록되었습니다.", response);
    }

    @PutMapping("/{quizId}")
    @Operation(summary = "퀴즈 수정", description = "등록된 퀴즈의 제목/문항/강사 등을 수정합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<AdminQuizMutationResponse>> updateQuiz(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId,
            @RequestBody(required = false) AdminQuizCreateRequest request
    ) {
        AdminQuizMutationResponse response = new AdminQuizMutationResponse(
                quizId,
                request == null || request.quizTitle() == null ? "React 기초 개념 퀴즈" : request.quizTitle(),
                request == null || request.instructorName() == null ? "안현" : request.instructorName(),
                request == null || request.questions() == null ? 1 : request.questions().size(),
                OffsetDateTime.parse("2026-06-22T16:00:00+09:00")
        );

        return ApiResponse.success("퀴즈가 수정되었습니다.", response);
    }

    @DeleteMapping("/{quizId}")
    @Operation(summary = "퀴즈 삭제", description = "등록된 퀴즈를 삭제합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId
    ) {
        return ApiResponse.successNoContent("퀴즈가 삭제되었습니다.");
    }

    @GetMapping("/{quizId}/statistics")
    @Operation(summary = "퀴즈 점수 현황 조회", description = "퀴즈의 응시 현황, 점수 분포, 수강생별 점수를 조회합니다. ADMIN 권한 필요. (Mock)")
    public ResponseEntity<ApiResponse<QuizController.InstructorQuizStatisticsResponse>> getQuizStatistics(
            @Parameter(description = "퀴즈 ID", example = "90") @PathVariable Long quizId
    ) {
        QuizController.InstructorQuizStatisticsResponse response = new QuizController.InstructorQuizStatisticsResponse(
                "React 완벽 가이드",
                "1주차: React 기초 개념",
                "React 기초 개념 퀴즈",
                new QuizController.InstructorQuizStatisticsResponse.Summary(6, 5, 1, 80),
                List.of(
                        new QuizController.InstructorQuizStatisticsResponse.ScoreDistribution("90~100", 2, 40),
                        new QuizController.InstructorQuizStatisticsResponse.ScoreDistribution("70~89", 2, 40),
                        new QuizController.InstructorQuizStatisticsResponse.ScoreDistribution("50~69", 1, 20),
                        new QuizController.InstructorQuizStatisticsResponse.ScoreDistribution("0~49", 0, 0)
                ),
                List.of(
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("choiyea2026", "최예아", true, 100, OffsetDateTime.parse("2026-05-10T00:00:00+09:00")),
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("kimminsu92", "김민수", true, 90, OffsetDateTime.parse("2026-05-12T00:00:00+09:00")),
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("leesujin01", "이수진", true, 80, OffsetDateTime.parse("2026-05-13T00:00:00+09:00")),
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("parkjihyun7", "박지현", true, 70, OffsetDateTime.parse("2026-05-11T00:00:00+09:00")),
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("jungyumin5", "정유민", true, 60, OffsetDateTime.parse("2026-05-14T00:00:00+09:00")),
                        new QuizController.InstructorQuizStatisticsResponse.StudentScore("hanseoyeong3", "한서영", false, null, null)
                )
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

    @Schema(description = "퀴즈 등록/수정 요청")
    public record AdminQuizCreateRequest(
            @Schema(description = "퀴즈 제목", example = "React 기초 개념 퀴즈") String quizTitle,
            @Schema(description = "강사 ID", example = "1") Long instructorId,
            @Schema(description = "강사명", example = "안현") String instructorName,
            @Schema(description = "연결 강의 ID", example = "1") Long courseId,
            @Schema(description = "연결 섹션(주차) ID", example = "1") Long sectionId,
            @Schema(description = "문제 목록") List<Question> questions
    ) {
        @Schema(description = "문제")
        public record Question(
                @Schema(description = "문제 내용", example = "React의 가상 DOM이란 무엇인가요?") String questionText,
                @Schema(description = "정답 보기 번호(1~4)", example = "2") Long correctOptionId,
                @Schema(description = "보기 목록 (4개)") List<Option> options
        ) {
        }

        @Schema(description = "보기")
        public record Option(
                @Schema(description = "보기 내용", example = "메모리에 존재하는 DOM의 표현") String optionText
        ) {
        }
    }

    @Schema(description = "퀴즈 등록/수정 결과")
    public record AdminQuizMutationResponse(
            @Schema(description = "퀴즈 ID", example = "90") Long quizId,
            @Schema(description = "퀴즈 제목", example = "React 기초 개념 퀴즈") String quizTitle,
            @Schema(description = "강사명", example = "안현") String instructorName,
            @Schema(description = "문제 수", example = "1") int questionCount,
            @Schema(description = "수정일시") OffsetDateTime updatedAt
    ) {
    }
}
