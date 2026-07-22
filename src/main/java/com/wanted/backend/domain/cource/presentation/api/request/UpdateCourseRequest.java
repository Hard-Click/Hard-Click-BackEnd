package com.wanted.backend.domain.cource.presentation.api.request;

import com.wanted.backend.domain.cource.application.command.UpdateCourseCommand;
import com.wanted.backend.domain.cource.domain.model.CourseLevel;
import com.wanted.backend.domain.cource.domain.model.PriceType;
import com.wanted.backend.global.domain.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "강의 수정 요청")
public record UpdateCourseRequest(
        @Schema(description = "강의명", example = "2027 수능 수학Ⅱ 미적분 실전 킬러 특강 (개정판)")
        @NotBlank(message = "강의명은 필수입니다.")
        String title,

        @Schema(description = "과목명 (SubjectType enum 값)", example = "MATH_1")
        @NotNull(message = "과목은 필수입니다.")
        SubjectType subject,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.png")
        String thumbnailUrl,

        @Schema(description = "가격 유형 (FREE / PAID)", example = "PAID")
        @NotNull(message = "가격 유형은 필수입니다.")
        PriceType priceType,

        @Schema(description = "가격 (원, 무료 강의는 0)", example = "89000")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,

        @Schema(description = "섹션(챕터) 목록. 강의 수정은 전체 커리큘럼 교체 방식이므로 기존 커리큘럼을 모두 포함해 보내야 합니다. 전체 삭제 시 빈 배열 [] 전달.")
        @NotNull(message = "커리큘럼(sections)은 필수입니다. 강의 수정은 전체 커리큘럼 교체 방식이므로 기존 커리큘럼을 모두 포함해 보내야 합니다. (전체 삭제가 필요하면 빈 배열 []을 전달)")
        @Valid
        List<UpdateSectionRequest> sections,

        @Schema(description = "학습 목표 목록", example = "[\"미적분 킬러 문제 유형을 파악한다\"]")
        List<String> learningObjectives,

        @Schema(description = "추천 대상 목록", example = "[\"수능 수학 3~4등급이며 1등급을 목표로 하는 수험생\"]")
        List<String> targetAudience,

        @Schema(description = "기술 태그 목록", example = "[\"미분법\", \"적분법\", \"극한\"]")
        List<String> techTags,

        @Schema(description = "난이도 (입문 / 중급 / 심화)", example = "중급")
        @NotNull(message = "난이도는 필수입니다.")
        CourseLevel level,

        @Schema(description = "권장 완강 기간(주). CP-SAT 스케줄러 입력.", example = "12")
        @Min(value = 1, message = "권장 완강 기간은 1주 이상이어야 합니다.")
        @Max(value = 52, message = "권장 완강 기간은 최대 52주입니다.")
        Integer recommendedWeeks,

        @Schema(description = "코스별 강도 상한(하루 최대 학습 분). 비우면 120분 적용.", example = "120")
        @Min(value = 1, message = "강도 상한은 1분 이상이어야 합니다.")
        @Max(value = 1440, message = "강도 상한은 하루 최대 분(1440분)을 초과할 수 없습니다.")
        Integer dailyMaxMinutes
) {
    public UpdateCourseCommand toCommand(Long courseId, Long requesterId) {
        List<UpdateCourseCommand.SectionCommand> sectionCommands = sections == null
                ? new ArrayList<>()
                : sections.stream()
                        .map(s -> {
                            List<UpdateCourseCommand.LessonCommand> lessonCommands = s.lessons() == null
                                    ? new ArrayList<>()
                                    : s.lessons().stream()
                                            .map(l -> new UpdateCourseCommand.LessonCommand(
                                                    l.lessonId(), l.title(), l.description(), l.orderIndex(), l.durationSeconds()))
                                            .toList();
                            return new UpdateCourseCommand.SectionCommand(
                                    s.sectionId(), s.title(), s.orderIndex(), lessonCommands);
                        })
                        .toList();

        return new UpdateCourseCommand(courseId, requesterId, title, subject.name(),
                thumbnailUrl, priceType, price, sectionCommands,
                learningObjectives, targetAudience, techTags, level.getLabel(),
                recommendedWeeks, dailyMaxMinutes);
    }
}
