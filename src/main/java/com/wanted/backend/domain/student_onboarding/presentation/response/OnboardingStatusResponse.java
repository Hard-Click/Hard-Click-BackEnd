package com.wanted.backend.domain.student_onboarding.presentation.response;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "온보딩 진행 상태 - 재진입 시 이어할 단계 판단용")
public record OnboardingStatusResponse(

        @Schema(description = "1단계(초기 설정) 완료 여부", example = "true")
        boolean profileCompleted,

        @Schema(description = "2단계(불가능 시간) 완료 여부", example = "true")
        boolean availabilityCompleted,

        @Schema(description = "3단계(모의고사 성적) 완료 여부", example = "false")
        boolean examScoreCompleted,

        @Schema(description = "온보딩 전체 완료 여부. false 면 스케줄러가 콜드스타트 기본값으로 계획한다.",
                example = "false")
        boolean onboarded,

        @Schema(description = "가용시간에서 유도된 하루 학습 상한(분). 2단계 전이면 null.", example = "300")
        Integer dailyCapMin,

        @Schema(description = "휴식 요일 비트마스크 (bit0=일 ... bit6=토). 예: 65 = 일·토 휴식", example = "65")
        int restDays
) {

    public static OnboardingStatusResponse from(OnboardingDtos.OnboardingStatusView view) {
        return new OnboardingStatusResponse(
                view.profileCompleted(),
                view.availabilityCompleted(),
                view.examScoreCompleted(),
                view.onboarded(),
                view.dailyCapMin(),
                view.restDays()
        );
    }
}
