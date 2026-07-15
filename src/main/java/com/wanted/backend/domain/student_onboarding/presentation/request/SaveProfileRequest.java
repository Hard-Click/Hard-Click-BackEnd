package com.wanted.backend.domain.student_onboarding.presentation.request;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;
import com.wanted.backend.domain.student_onboarding.domain.model.AdmissionStrategy;
import com.wanted.backend.domain.student_onboarding.domain.model.ExplorationTrack;
import com.wanted.backend.domain.student_onboarding.domain.model.StudyPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "온보딩 1단계 - 학습 스케줄 초기 설정")
public record SaveProfileRequest(

        @Schema(description = "목표 대학", example = "서울대학교")
        @Size(max = 100, message = "목표 대학은 100자를 초과할 수 없습니다.")
        String targetUniversity,

        @Schema(description = "목표 학과", example = "컴퓨터공학부")
        @Size(max = 100, message = "목표 학과는 100자를 초과할 수 없습니다.")
        String targetMajor,

        @Schema(description = "입시 전략", example = "UNDECIDED",
                allowableValues = {"REGULAR", "EARLY", "UNDECIDED"})
        @NotNull(message = "입시 전략은 필수입니다.")
        AdmissionStrategy admissionStrategy,

        @Schema(description = "국어 선택과목", example = "SPEECH_WRITING",
                allowableValues = {"SPEECH_WRITING", "LANGUAGE_MEDIA"})
        @Size(max = 30)
        String koreanElective,

        @Schema(description = "수학 선택과목", example = "CALCULUS",
                allowableValues = {"CALCULUS", "GEOMETRY", "STATISTICS"})
        @Size(max = 30)
        String mathElective,

        @Schema(description = "탐구 계열", example = "SOCIAL",
                allowableValues = {"SOCIAL", "SCIENCE", "MIXED"})
        ExplorationTrack explorationTrack,

        @Schema(description = "탐구 과목 1", example = "세계지리")
        @Size(max = 40)
        String explorationSubject1,

        @Schema(description = "탐구 과목 2", example = "세계사")
        @Size(max = 40)
        String explorationSubject2,

        @Schema(description = "제2외국어/한문 응시 여부", example = "false")
        boolean secondLanguage,

        @Schema(description = "학습 성향", example = "NONE",
                allowableValues = {"MORNING", "EVENING", "NONE"})
        @NotNull(message = "학습 성향은 필수입니다.")
        StudyPreference studyPreference
) {

    public OnboardingDtos.SaveProfileCommand toCommand() {
        return new OnboardingDtos.SaveProfileCommand(
                targetUniversity,
                targetMajor,
                admissionStrategy,
                koreanElective,
                mathElective,
                explorationTrack,
                explorationSubject1,
                explorationSubject2,
                secondLanguage,
                studyPreference
        );
    }
}
