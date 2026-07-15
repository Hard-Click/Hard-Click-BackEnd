package com.wanted.backend.domain.student_onboarding.presentation.request;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 온보딩 2단계 - 불가능한 시간 체크.
 *
 * <p>화면의 30분 단위 주간 그리드를 그대로 옮긴 형태다. 체크된(=불가능한) 칸만 보낸다.
 * 요일이 목록에 없거나 slots 가 비어 있으면 그 요일은 종일 가능으로 해석한다.
 */
@Schema(description = "온보딩 2단계 - 불가능한 시간 체크")
public record SaveAvailabilityRequest(

        @Schema(description = "요일별 불가능 시간대. 체크된 칸만 보낸다.")
        @NotNull(message = "불가능 시간 목록은 필수입니다.")
        List<@Valid DayUnavailability> unavailable
) {

    @Schema(description = "한 요일의 불가능 슬롯")
    public record DayUnavailability(

            @Schema(description = "요일 (0=일, 1=월 ... 6=토)", example = "1")
            @NotNull(message = "요일은 필수입니다.")
            Integer dayOfWeek,

            @Schema(description = "불가능한 30분 슬롯 인덱스 목록. 0=00:00~00:30 ... 47=23:30~24:00",
                    example = "[16, 17, 18, 19]")
            @NotNull(message = "슬롯 목록은 필수입니다.")
            Set<Integer> slots
    ) {
    }

    public OnboardingDtos.SaveAvailabilityCommand toCommand() {
        Map<Integer, Set<Integer>> byDay = new HashMap<>();
        for (DayUnavailability day : unavailable) {
            // 같은 요일이 두 번 오면 합집합으로 병합한다(클라이언트가 나눠 보내도 안전하게).
            byDay.merge(day.dayOfWeek(), day.slots(), (a, b) -> {
                Set<Integer> merged = new java.util.TreeSet<>(a);
                merged.addAll(b);
                return merged;
            });
        }
        return new OnboardingDtos.SaveAvailabilityCommand(byDay);
    }
}
