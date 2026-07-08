package com.wanted.backend.domain.study.presentation.request;

import com.wanted.backend.global.domain.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStudyRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 300, message = "제목은 300자 이하여야 합니다.")
        String title,


        @Schema(description = "과목 (SubjectType enum 값)", example = "MATH_1")
        @NotNull(message = "과목은 필수입니다.")
        SubjectType subject,

        @Min(value = 2, message = "최소 2명 이상이어야 합니다.")
        int maxCount,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {}
