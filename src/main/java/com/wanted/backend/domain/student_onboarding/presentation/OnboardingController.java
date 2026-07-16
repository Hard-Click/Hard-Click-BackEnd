package com.wanted.backend.domain.student_onboarding.presentation;

import com.wanted.backend.domain.student_onboarding.application.usecase.OnboardingUseCase;
import com.wanted.backend.domain.student_onboarding.presentation.request.SaveAvailabilityRequest;
import com.wanted.backend.domain.student_onboarding.presentation.request.SaveExamScoresRequest;
import com.wanted.backend.domain.student_onboarding.presentation.request.SaveProfileRequest;
import com.wanted.backend.domain.student_onboarding.presentation.response.OnboardingStatusResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학습 스케줄 온보딩 API - 구독권 결제 완료 직후 '스케줄 입력하기' 플로우.
 *
 * <p>3개 화면을 순서대로 저장하지만 각 단계는 독립적인 덮어쓰기(PUT)라 순서를 강제하지 않는다.
 * 세 단계가 다 차면 온보딩 완료로 표시된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onboarding")
@Tag(name = "Study Onboarding", description = "학습 스케줄 온보딩 API (학생)")
public class OnboardingController {

    private final OnboardingUseCase onboardingUseCase;

    @Operation(summary = "온보딩 진행 상태 조회",
            description = "어느 단계까지 저장됐는지 조회합니다. 중간 이탈 후 재진입 시 이어할 단계를 판단하는 데 씁니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(
                "온보딩 상태를 조회했습니다.",
                OnboardingStatusResponse.from(onboardingUseCase.getStatus(userDetails.getMemberId())));
    }

    @Operation(summary = "온보딩 1단계 - 학습 스케줄 초기 설정",
            description = "목표 대학·학과, 입시 전략, 선택과목, 탐구 계열, 학습 성향을 저장합니다. 다시 호출하면 덮어씁니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> saveProfile(
            @Valid @RequestBody SaveProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        onboardingUseCase.saveProfile(userDetails.getMemberId(), request.toCommand());
        return ApiResponse.successNoContent("초기 설정이 저장되었습니다.");
    }

    @Operation(summary = "온보딩 2단계 - 불가능한 시간 체크",
            description = "30분 단위 주간 그리드에서 체크한 불가능 시간을 저장합니다. "
                    + "서버가 가용 구간·휴식 요일·하루 학습 상한으로 변환해 반영합니다. 다시 호출하면 전체 덮어씁니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "요일/슬롯 범위 오류(OB001·OB002) 또는 모든 요일이 불가능(OB003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<Void>> saveAvailability(
            @Valid @RequestBody SaveAvailabilityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        onboardingUseCase.saveAvailability(userDetails.getMemberId(), request.toCommand());
        return ApiResponse.successNoContent("불가능한 시간이 저장되었습니다.");
    }

    @Operation(summary = "온보딩 3단계 - 최근 모의고사 성적",
            description = "응시영역별 원점수를 저장합니다. 서버가 등급(1~9)으로 변환해 함께 보관합니다. "
                    + "같은 응시일로 다시 호출하면 덮어씁니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "영역 중복(OB004), 원점수 범위 초과(OB005), 미래 응시일(OB006)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/exam-scores")
    public ResponseEntity<ApiResponse<Void>> saveExamScores(
            @Valid @RequestBody SaveExamScoresRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        onboardingUseCase.saveExamScores(userDetails.getMemberId(), request.toCommand());
        return ApiResponse.successNoContent("모의고사 성적이 저장되었습니다.");
    }
}
