package com.wanted.backend.domain.study_schedule.presentation;

import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import com.wanted.backend.domain.study_schedule.presentation.response.ScheduleResponses;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "Study Schedule", description = "학습 스케줄 API (학생)")
public class ScheduleController {

    private final ScheduleUseCase scheduleUseCase;

    @GetMapping("/me")
    @Operation(summary = "내 학습 스케줄 조회", description = "기간(from~to) 내 활성 스케줄을 캘린더용으로 조회합니다. 미지정 시 이번 달.")
    public ResponseEntity<ApiResponse<List<ScheduleResponses.CalendarItemResponse>>> getMySchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회 시작일(ISO). 미지정 시 이번 달 1일", example = "2026-07-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일(ISO). 미지정 시 이번 달 말일", example = "2026-07-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate today = LocalDate.now();
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : today.withDayOfMonth(today.lengthOfMonth());

        List<ScheduleResponses.CalendarItemResponse> body =
                scheduleUseCase.getMySchedule(userDetails.getMemberId(), start, end).stream()
                        .map(ScheduleResponses.CalendarItemResponse::from)
                        .toList();
        return ApiResponse.success("학습 스케줄 조회 성공", body);
    }

    @GetMapping("/me/today")
    @Operation(summary = "오늘 할 일", description = "오늘 날짜의 스케줄 항목과 진행률(완료/전체)을 조회합니다.")
    public ResponseEntity<ApiResponse<ScheduleResponses.TodayResponse>> getMyToday(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(
                "오늘 할 일 조회 성공",
                ScheduleResponses.TodayResponse.from(
                        scheduleUseCase.getMyToday(userDetails.getMemberId(), LocalDate.now())));
    }

    @PatchMapping("/slots/{slotId}/complete")
    @Operation(summary = "스케줄 항목 완료 체크", description = "본인 스케줄 슬롯을 완료(DONE) 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> completeSlot(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "슬롯 ID", example = "9012") @PathVariable Long slotId
    ) {
        scheduleUseCase.completeSlot(userDetails.getMemberId(), slotId);
        return ApiResponse.successNoContent("완료 처리되었습니다.");
    }
}
