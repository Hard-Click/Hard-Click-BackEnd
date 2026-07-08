package com.wanted.backend.domain.study.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.study.application.result.StudyItemResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "스터디 목록 응답")
public record StudyListResponse(
        @Schema(description = "스터디 목록")
        List<StudyItem> content,
        @Schema(description = "전체 페이지 수", example = "3")
        int totalPages
) {
    public static StudyListResponse from(StudyListResult result) {
        return new StudyListResponse(
                result.items().stream().map(StudyItem::from).toList(),
                result.totalPages()
        );
    }

    @Schema(description = "스터디 목록 항목")
    public record StudyItem(
            @Schema(description = "스터디 그룹 ID", example = "101")
            Long groupId,
            @Schema(description = "스터디 제목", example = "주말 React 스터디 모집")
            String title,
            @Schema(description = "스터디 내용", example = "강남 카페에서 진행합니다")
            String content,
            @Schema(description = "작성자 이름", example = "최*진")
            String authorName,
            @Schema(description = "과목명", example = "수학1")
            String subjectName,
            @Schema(description = "현재 참여 인원", example = "3")
            int currentCount,
            @Schema(description = "최대 참여 인원", example = "6")
            int maxCount,
            @Schema(description = "모집 마감 여부", example = "false")
            boolean isClosed,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
            @Schema(description = "작성일시", example = "2025-03-15T14:30:00")
            LocalDateTime createdAt
    ) {
        public static StudyItem from(StudyItemResult r) {
            return new StudyItem(
                    r.studyId(), r.title(), r.content(), r.authorName(), r.subjectName(),
                    r.currentCount(), r.maxCount(), r.isClosed(), r.createdAt()
            );
        }
    }
}
