package com.wanted.backend.domain.student_onboarding.presentation.request;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;
import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 온보딩 3단계 - 최근 모의고사 성적.
 *
 * <p>원점수를 받아 서버가 등급으로 변환한다. 영역별 만점이 달라(국어·수학·영어 100 / 한국사·탐구 50)
 * 상한 검증은 Bean Validation 이 아니라 도메인(GradeCutPolicy)에서 한다 - 초과 시 OB005.
 */
@Schema(description = "온보딩 3단계 - 최근 모의고사 성적")
public record SaveExamScoresRequest(

        @Schema(description = "응시일(ISO). 미지정 시 오늘로 저장한다.", example = "2026-06-04")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate examDate,

        @Schema(description = "응시영역별 성적. 입력한 영역만 보낸다.")
        @NotEmpty(message = "성적은 최소 한 과목 이상 입력해야 합니다.")
        List<@Valid ExamScore> scores
) {

    @Schema(description = "응시영역 하나의 성적")
    public record ExamScore(

            @Schema(description = "응시영역", example = "KOREAN",
                    allowableValues = {"KOREAN", "MATH", "ENGLISH", "HISTORY", "EXPLORATION_1", "EXPLORATION_2"})
            @NotNull(message = "응시영역은 필수입니다.")
            SubjectArea subjectArea,

            @Schema(description = "응시과목명. 영어/한국사는 선택과목이 없어 무시된다.", example = "화법과 작문")
            String subjectName,

            @Schema(description = "원점수. 영역별 만점 이내여야 한다(국어·수학·영어 100, 한국사·탐구 50).",
                    example = "88")
            @NotNull(message = "원점수는 필수입니다.")
            @Min(value = 0, message = "원점수는 0점 이상이어야 합니다.")
            Integer rawScore
    ) {
    }

    public OnboardingDtos.SaveExamScoresCommand toCommand() {
        return new OnboardingDtos.SaveExamScoresCommand(
                examDate,
                scores.stream()
                        .map(s -> new OnboardingDtos.ExamScoreEntry(
                                s.subjectArea(), s.subjectName(), s.rawScore()))
                        .toList()
        );
    }
}
