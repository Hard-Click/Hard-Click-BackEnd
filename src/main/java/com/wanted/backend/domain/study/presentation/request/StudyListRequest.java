package com.wanted.backend.domain.study.presentation.request;

import com.wanted.backend.global.domain.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StudyListRequest(
        @Schema(description = "과목 필터 (SubjectType enum 값)", example = "MATH_1")
        SubjectType subject,

        @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Schema(description = "조회 크기", example = "10")
        @Min(value = 1, message = "조회 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "조회 크기는 50 이하여야 합니다.")
        Integer size
) {
    public StudyListRequest {
        if (page == null) page = 0;
        if (size == null) size = 10;
    }
}
