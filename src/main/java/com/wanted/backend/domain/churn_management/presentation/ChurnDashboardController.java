package com.wanted.backend.domain.churn_management.presentation;

import com.wanted.backend.domain.churn_management.application.usecase.ChurnActionUseCase;
import com.wanted.backend.domain.churn_management.application.usecase.GetChurnDashboardUseCase;
import com.wanted.backend.domain.churn_management.presentation.response.ChurnResponses;
import com.wanted.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/churn")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Churn Management", description = "이탈관리 대시보드 API (관리자)")
public class ChurnDashboardController {

    private final GetChurnDashboardUseCase getChurnDashboardUseCase;
    private final ChurnActionUseCase churnActionUseCase;

    @GetMapping("/summary")
    @Operation(summary = "이탈관리 요약", description = "고위험/중위험 학생 수, 이번 주 신규 위험, 평균 위험 점수를 조회합니다.")
    public ResponseEntity<ApiResponse<ChurnResponses.SummaryResponse>> getSummary() {
        return ApiResponse.success(
                "이탈관리 요약 조회 성공",
                ChurnResponses.SummaryResponse.from(getChurnDashboardUseCase.getSummary()));
    }

    @GetMapping("/trend")
    @Operation(summary = "이탈 위험 학생 추이", description = "최근 N주간 주별 고위험 학생 수 추이를 조회합니다. 기본 8주.")
    public ResponseEntity<ApiResponse<List<ChurnResponses.TrendPointResponse>>> getTrend(
            @Parameter(description = "조회할 주 수", example = "8") @RequestParam(defaultValue = "8") int weeks
    ) {
        List<ChurnResponses.TrendPointResponse> body = getChurnDashboardUseCase.getTrend(weeks).stream()
                .map(ChurnResponses.TrendPointResponse::from)
                .toList();
        return ApiResponse.success("이탈 위험 추이 조회 성공", body);
    }

    @GetMapping("/reasons")
    @Operation(summary = "주요 이탈 사유", description = "위험군 학생의 대표 사유별 비율을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChurnResponses.ReasonResponse>>> getReasons() {
        List<ChurnResponses.ReasonResponse> body = getChurnDashboardUseCase.getReasons().stream()
                .map(ChurnResponses.ReasonResponse::from)
                .toList();
        return ApiResponse.success("이탈 사유 조회 성공", body);
    }

    @GetMapping("/students")
    @Operation(summary = "이탈 위험 학생 목록", description = "위험도(level)로 필터링하여 학생 목록을 페이징 조회합니다. level 미지정 시 위험군(중위험 이상) 전체.")
    public ResponseEntity<ApiResponse<ChurnResponses.StudentPageResponse>> getStudents(
            @Parameter(description = "위험도 필터 (HIGH / MEDIUM). 미지정 시 전체 위험군", example = "HIGH")
            @RequestParam(required = false) String level,
            @Parameter(description = "페이지(0-base)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "이탈 위험 학생 목록 조회 성공",
                ChurnResponses.StudentPageResponse.from(
                        getChurnDashboardUseCase.getStudents(level, page, size)));
    }

    @GetMapping("/students/{enrollmentId}")
    @Operation(summary = "학생 위험 상세", description = "위험 점수 기여 요인과 학습 현황을 포함한 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<ChurnResponses.StudentDetailResponse>> getStudentDetail(
            @Parameter(description = "수강 ID", example = "1063") @PathVariable Long enrollmentId
    ) {
        return ApiResponse.success(
                "학생 위험 상세 조회 성공",
                ChurnResponses.StudentDetailResponse.from(
                        getChurnDashboardUseCase.getStudentDetail(enrollmentId)));
    }

    @PostMapping("/students/{enrollmentId}/nudge")
    @Operation(summary = "독려 알림 보내기", description = "이탈 위험 학생에게 학습 독려 알림을 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> nudge(
            @Parameter(description = "수강 ID", example = "1063") @PathVariable Long enrollmentId
    ) {
        churnActionUseCase.nudge(enrollmentId);
        return ApiResponse.successNoContent("독려 알림을 보냈습니다.");
    }

    @PostMapping("/students/{enrollmentId}/reflow")
    @Operation(summary = "스케줄 재조정 권유", description = "이탈 위험 학생에게 스케줄 재조정을 권유하는 알림을 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> suggestReflow(
            @Parameter(description = "수강 ID", example = "1063") @PathVariable Long enrollmentId
    ) {
        churnActionUseCase.suggestReflow(enrollmentId);
        return ApiResponse.successNoContent("스케줄 재조정 권유 알림을 보냈습니다.");
    }
}
