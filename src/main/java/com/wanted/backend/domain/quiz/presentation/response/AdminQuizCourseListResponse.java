package com.wanted.backend.domain.quiz.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "관리자 퀴즈 관리 강의 목록")
public record AdminQuizCourseListResponse(List<AdminQuizCourseItem> courses) {

    @Schema(description = "퀴즈 관리 강의 항목")
    public record AdminQuizCourseItem(
            @Schema(description = "강의 ID", example = "1") Long courseId,
            @Schema(description = "강의명", example = "React 완벽 가이드") String courseTitle,
            @Schema(description = "공개 여부", example = "true") boolean visible,
            @Schema(description = "수강생 수", example = "89") int studentCount,
            @Schema(description = "강사명", example = "안현") String instructorName,
            @Schema(description = "강의 등록일시") OffsetDateTime registeredAt
    ) {
    }
}
