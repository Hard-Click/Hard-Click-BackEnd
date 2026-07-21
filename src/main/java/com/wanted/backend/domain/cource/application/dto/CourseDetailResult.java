package com.wanted.backend.domain.cource.application.dto;

import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.PriceType;

import java.util.List;

public record CourseDetailResult(
        Long courseId,
        String title,
        String subject,
        String description,
        String thumbnailUrl,
        PriceType priceType,
        int price,
        CourseStatus status,
        String instructorName,
        double rating,
        int reviewCount,
        int studentCount,
        int instructorStudentCount,
        int instructorCourseCount,
        double instructorRating,
        String instructorOneLineIntro,
        String instructorIntroduction,
        String instructorCareer,
        List<SectionResult> sections,
        List<String> learningObjectives,
        List<String> targetAudience,
        List<String> techTags,
        String level,
        // 학습 정책(강의 수정 화면 프리필용). 미설정(구 강의) 시 null.
        Integer recommendedWeeks,
        Integer dailyMaxMinutes
) {
    public record SectionResult(
            Long sectionId,
            String title,
            int orderIndex,
            List<LessonResult> lessons
    ) {}

    public record LessonResult(
            Long lessonId,
            String title,
            String description,
            int orderIndex,
            Integer durationSeconds,
            boolean isPreview
    ) {}
}
