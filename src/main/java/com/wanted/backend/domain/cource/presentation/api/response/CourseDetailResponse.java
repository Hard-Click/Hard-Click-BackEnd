package com.wanted.backend.domain.cource.presentation.api.response;

import com.wanted.backend.domain.cource.application.dto.CourseDetailResult;
import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.PriceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "강의 상세 응답")
public record CourseDetailResponse(
        @Schema(description = "강의 ID", example = "1")
        Long courseId,

        @Schema(description = "강의명", example = "2027 수능 수학Ⅱ 미적분 실전 킬러 특강")
        String title,

        @Schema(description = "과목명", example = "수학Ⅱ")
        String subjectName,

        @Schema(description = "강의 설명", example = "수능 수학Ⅱ 미적분 단원의 킬러 문제를 완전 정복하는 특강입니다.")
        String description,

        @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.png")
        String thumbnailUrl,

        @Schema(description = "가격 유형 (FREE / PAID)", example = "PAID")
        PriceType priceType,

        @Schema(description = "가격 (원)", example = "89000")
        int price,

        @Schema(description = "가격 표시 (무료 or N원)", example = "89,000원")
        String priceLabel,

        @Schema(description = "강의 상태 (DRAFT / PUBLISHED)", example = "PUBLISHED")
        CourseStatus status,

        @Schema(description = "강사명", example = "박지훈")
        String instructorName,

        @Schema(description = "평균 평점", example = "4.8")
        double averageRating,

        @Schema(description = "리뷰 수", example = "1234")
        int reviewCount,

        @Schema(description = "수강생 수", example = "12543")
        int studentCount,

        @Schema(description = "커리큘럼 섹션 목록")
        List<SectionResponse> sections,

        @Schema(description = "학습 목표 목록", example = "[\"미적분 킬러 문제 유형을 파악한다\"]")
        List<String> learningObjectives,

        @Schema(description = "추천 대상 목록", example = "[\"수능 수학 3~4등급 수험생\"]")
        List<String> targetAudience,

        @Schema(description = "기술 태그 목록", example = "[\"미분법\", \"적분법\"]")
        List<String> techTags,

        @Schema(description = "난이도", example = "중급~고급")
        String level,

        @Schema(description = "강사 전체 수강생 수", example = "42800")
        int instructorStudentCount,

        @Schema(description = "강사 강의 수", example = "7")
        int instructorCourseCount,

        @Schema(description = "강사 평균 평점", example = "4.9")
        double instructorRating,

        @Schema(description = "강사 한줄소개", example = "수능 수학 전문 강사")
        String instructorOneLineIntro,

        @Schema(description = "강사 자기소개", example = "서울대학교 수학교육과 출신으로 15년간 수능 수학을 지도해온 베테랑 강사입니다.")
        String instructorIntroduction,

        @Schema(description = "강사 경력", example = "전 대성마이맥 수학 강사\n전 메가스터디 인기 강사\n현 FLOWN 수학 대표 강사")
        String instructorCareer,

        @Schema(description = "권장 완강 기간(주). 미설정(구 강의) 시 null.", example = "12")
        Integer recommendedWeeks,

        @Schema(description = "코스별 강도 상한(하루 최대 학습 분). 미설정(구 강의) 시 null.", example = "120")
        Integer dailyMaxMinutes
) {
    @Schema(description = "섹션(챕터) 응답")
    public record SectionResponse(
            @Schema(description = "섹션 ID", example = "1")
            Long sectionId,

            @Schema(description = "섹션 제목", example = "섹션 1: 함수의 극한")
            String title,

            @Schema(description = "섹션 순서 (0-based)", example = "0")
            int orderIndex,

            @Schema(description = "레슨 목록")
            List<LessonResponse> lessons
    ) {}

    @Schema(description = "레슨(회차) 응답")
    public record LessonResponse(
            @Schema(description = "레슨 ID", example = "1")
            Long lessonId,

            @Schema(description = "재생/진도 API에 사용하는 영상 ID (lessonId와 동일)", example = "1")
            Long videoId,

            @Schema(description = "레슨 제목", example = "OT 및 학습 방향")
            String title,

            @Schema(description = "레슨 설명", example = "강의 전체 구성과 학습 방향을 안내합니다.")
            String description,

            @Schema(description = "레슨 순서 (0-based)", example = "0")
            int orderIndex,

            @Schema(description = "영상 재생시간 (초)", example = "323")
            Integer durationSeconds,

            @Schema(description = "미리보기 가능 여부 (첫 번째 섹션의 첫 번째 레슨만 true)", example = "true")
            boolean isPreview
    ) {}

    public static CourseDetailResponse from(CourseDetailResult result) {
        String priceLabel = result.priceType() == PriceType.FREE
                ? "무료"
                : String.format("%,d원", result.price());

        List<SectionResponse> sections = result.sections().stream()
                .map(s -> new SectionResponse(
                        s.sectionId(),
                        s.title(),
                        s.orderIndex(),
                        s.lessons().stream()
                                .map(l -> new LessonResponse(
                                        l.lessonId(),
                                        l.lessonId(),
                                        l.title(),
                                        l.description(),
                                        l.orderIndex(),
                                        l.durationSeconds(),
                                        l.isPreview()
                                ))
                                .toList()
                ))
                .toList();

        return new CourseDetailResponse(
                result.courseId(),
                result.title(),
                result.subject(),
                result.description(),
                result.thumbnailUrl(),
                result.priceType(),
                result.price(),
                priceLabel,
                result.status(),
                result.instructorName(),
                result.rating(),
                result.reviewCount(),
                result.studentCount(),
                sections,
                result.learningObjectives(),
                result.targetAudience(),
                result.techTags(),
                result.level(),
                result.instructorStudentCount(),
                result.instructorCourseCount(),
                result.instructorRating(),
                result.instructorOneLineIntro(),
                result.instructorIntroduction(),
                result.instructorCareer(),
                result.recommendedWeeks(),
                result.dailyMaxMinutes()
        );
    }
}
