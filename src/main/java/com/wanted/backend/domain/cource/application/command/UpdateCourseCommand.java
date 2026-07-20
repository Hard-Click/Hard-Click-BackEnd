package com.wanted.backend.domain.cource.application.command;

import com.wanted.backend.domain.cource.domain.model.PriceType;

import java.util.List;

public record UpdateCourseCommand(
        Long courseId,
        Long requesterId,
        String title,
        String subject,
        String thumbnailUrl,
        PriceType priceType,
        int price,
        List<SectionCommand> sections,
        List<String> learningObjectives,
        List<String> targetAudience,
        List<String> techTags,
        String level,
        Integer recommendedWeeks,
        Integer dailyMaxMinutes
) {
    public record SectionCommand(
            Long id,           // null = 신규 섹션
            String title,
            int orderIndex,
            List<LessonCommand> lessons
    ) {}

    public record LessonCommand(
            Long id,           // null = 신규 회차
            String title,
            String description,
            int orderIndex,
            Integer durationSeconds
    ) {}
}
