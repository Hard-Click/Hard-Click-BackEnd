package com.wanted.backend.domain.cource.application.command;

import com.wanted.backend.domain.cource.domain.model.PriceType;

import java.util.List;

public record CreateCourseCommand(
        Long authorId,
        String title,
        String subject,
        String description,
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
            String title,
            int orderIndex,
            List<LessonCommand> lessons
    ) {}

    public record LessonCommand(
            String title,
            String description,
            int orderIndex,
            Integer durationSeconds
    ) {}
}
