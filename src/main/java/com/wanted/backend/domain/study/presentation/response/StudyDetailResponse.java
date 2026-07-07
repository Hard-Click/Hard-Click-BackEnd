package com.wanted.backend.domain.study.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "스터디 상세 응답")
public record StudyDetailResponse(
        @Schema(description = "스터디 그룹 ID", example = "101")
        Long groupId,
        @Schema(description = "스터디 제목", example = "수학 1등급 목표 스터디")
        String title,
        @Schema(description = "스터디 내용", example = "매주 일요일 밤 10시에 모여서 질문 받습니다.")
        String content,
        @Schema(description = "과목명", example = "수학1")
        String subjectName,
        @Schema(description = "작성자 이름", example = "이*연")
        String authorName,
        @Schema(description = "현재 참여 인원", example = "2")
        int currentCount,
        @Schema(description = "최대 참여 인원", example = "5")
        int maxCount,
        @Schema(description = "본인이 작성한 스터디 여부", example = "false")
        boolean isMine,
        @Schema(description = "참여 여부", example = "true")
        boolean isJoined,
        @Schema(description = "모집 마감 여부", example = "false")
        boolean isClosed,
        @Schema(description = "참여 중인 회원 이름 목록 (참여자에게만 노출)")
        List<String> members,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
        @Schema(description = "작성일시", example = "2025-03-15T14:30:00")
        LocalDateTime createdAt
) {}
