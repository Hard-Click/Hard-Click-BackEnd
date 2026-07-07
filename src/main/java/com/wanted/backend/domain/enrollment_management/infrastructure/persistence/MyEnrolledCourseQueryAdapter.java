package com.wanted.backend.domain.enrollment_management.infrastructure.persistence;

import com.wanted.backend.domain.enrollment_management.application.port.MyEnrolledCourseQueryPort;
import com.wanted.backend.domain.enrollment_management.domain.model.Enrollment;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.enrollment_management.domain.repository.EnrollmentRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.course.CourseReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.course.CourseReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.lesson.EnrolledLessonReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.lesson.EnrolledLessonReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.progress.VideoProgressReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.progress.VideoProgressReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.section.EnrolledCourseSectionReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.section.EnrolledCourseSectionReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEnrolledCourseQueryAdapter implements MyEnrolledCourseQueryPort {

    // 여러 참조 테이블에서 가져온 조회 결과를 화면 응답에 필요한 포트 데이터로 조합한다.
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReferenceRepository courseRepository;
    private final EnrolledCourseSectionReferenceRepository sectionRepository;
    private final EnrolledLessonReferenceRepository lessonRepository;
    private final VideoProgressReferenceRepository progressRepository;

    @Override
    public List<MyEnrolledCourseData> findByMemberId(Long memberId) {
        List<Enrollment> enrollments = findMyCourseEnrollments(memberId);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .toList();

        // 강의/레슨/진도 정보를 한 번씩 조회한 뒤 courseId 기준으로 묶어서 N+1 조회를 피한다.
        Map<Long, CourseReferenceEntity> courseById = courseRepository.findByIdIn(courseIds).stream()
                .collect(Collectors.toMap(CourseReferenceEntity::getId, Function.identity()));
        Map<Long, List<EnrolledLessonReferenceEntity>> lessonsByCourseId = findLessonsByCourseId(courseIds);
        Map<Long, List<VideoProgressReferenceEntity>> progressesByCourseId = progressRepository
                .findByMemberIdAndCourseIdIn(memberId, courseIds).stream()
                .collect(Collectors.groupingBy(VideoProgressReferenceEntity::getCourseId));

        return enrollments.stream()
                .map(enrollment -> toData(
                        enrollment.getCourseId(),
                        courseById.get(enrollment.getCourseId()),
                        lessonsByCourseId.getOrDefault(enrollment.getCourseId(), List.of()),
                        progressesByCourseId.getOrDefault(enrollment.getCourseId(), List.of()),
                        enrollment.getEffectiveStatus()
                ))
                .toList();
    }

    private List<Enrollment> findMyCourseEnrollments(Long memberId) {
        return enrollmentRepository.findByMemberId(memberId).stream()
                .filter(enrollment -> EnrollmentStatus.myCourseListTargets().contains(enrollment.getEffectiveStatus()))
                .toList();
    }

    // 영상 재생 식별자(videoId)는 이제 lesson.id다 — 강의별로 섹션 순서 -> 레슨 순서를 보존해서 묶는다.
    private Map<Long, List<EnrolledLessonReferenceEntity>> findLessonsByCourseId(Collection<Long> courseIds) {
        List<EnrolledCourseSectionReferenceEntity> sections = sectionRepository
                .findByCourseIdInOrderByCourseIdAscOrderIndexAscIdAsc(courseIds);
        if (sections.isEmpty()) {
            return Map.of();
        }

        List<Long> sectionIds = sections.stream().map(EnrolledCourseSectionReferenceEntity::getId).toList();
        Map<Long, List<EnrolledLessonReferenceEntity>> lessonsBySectionId = lessonRepository
                .findBySectionIdInOrderByOrderIndexAscIdAsc(sectionIds).stream()
                .collect(Collectors.groupingBy(EnrolledLessonReferenceEntity::getSectionId));

        Map<Long, List<EnrolledLessonReferenceEntity>> lessonsByCourseId = new LinkedHashMap<>();
        for (EnrolledCourseSectionReferenceEntity section : sections) {
            List<EnrolledLessonReferenceEntity> nonPreviewLessons = lessonsBySectionId
                    .getOrDefault(section.getId(), List.of())
                    .stream()
                    .filter(lesson -> !isPreviewLesson(section, lesson))
                    .toList();
            lessonsByCourseId
                    .computeIfAbsent(section.getCourseId(), key -> new ArrayList<>())
                    .addAll(nonPreviewLessons);
        }
        return lessonsByCourseId;
    }

    private boolean isPreviewLesson(EnrolledCourseSectionReferenceEntity section, EnrolledLessonReferenceEntity lesson) {
        return section.getOrderIndex() == 0 && lesson.getOrderIndex() == 0;
    }

    private MyEnrolledCourseData toData(
            Long courseId,
            CourseReferenceEntity course,
            List<EnrolledLessonReferenceEntity> lessons,
            List<VideoProgressReferenceEntity> progresses,
            EnrollmentStatus enrollmentStatus
    ) {
        Set<Long> lessonIds = lessons.stream()
                .map(EnrolledLessonReferenceEntity::getId)
                .collect(Collectors.toSet());
        // 삭제/이동된 레슨의 진도 이력이 남아있어도 이어보기 값이 현재 목록 밖을 가리키지 않도록
        // 진도 후보를 현재 lessonIds로 제한한다.
        List<VideoProgressReferenceEntity> progressesForCurrentLessons = progresses.stream()
                .filter(progress -> lessonIds.contains(progress.getVideoId()))
                .toList();

        VideoProgressReferenceEntity lastProgress = findLastProgress(progressesForCurrentLessons);
        EnrolledLessonReferenceEntity firstLesson = lessons.isEmpty() ? null : lessons.get(0);

        return new MyEnrolledCourseData(
                courseId,
                course == null ? "(삭제된 강의)" : course.getTitle(),
                course == null ? null : course.getThumbnailUrl(),
                countCompletedLessons(progressesForCurrentLessons),
                lessons.size(),
                lastProgress == null ? null : lastProgress.getUpdatedAt(),
                lastProgress == null ? firstVideoId(firstLesson) : lastProgress.getVideoId(),
                lastProgress == null ? 0 : lastProgress.getLastPositionSeconds(),
                enrollmentStatus
        );
    }

    private Long firstVideoId(EnrolledLessonReferenceEntity firstLesson) {
        return firstLesson == null ? null : firstLesson.getId();
    }

    private Integer countCompletedLessons(List<VideoProgressReferenceEntity> progressesForCurrentLessons) {
        return (int) progressesForCurrentLessons.stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getCompleted()))
                .count();
    }

    private VideoProgressReferenceEntity findLastProgress(List<VideoProgressReferenceEntity> progresses) {
        return progresses.stream()
                .max(Comparator.comparing(VideoProgressReferenceEntity::getUpdatedAt))
                .orElse(null);
    }
}
