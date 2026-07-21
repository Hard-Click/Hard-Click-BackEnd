package com.wanted.backend.domain.cource.application.service;

import com.wanted.backend.domain.cource.application.dto.CourseDetailResult;
import com.wanted.backend.domain.cource.application.dto.CourseListResult;
import com.wanted.backend.domain.cource.application.dto.InstructorDashboardResult;
import com.wanted.backend.domain.cource.application.port.CourseLearningPolicyPort;
import com.wanted.backend.domain.cource.application.port.EnrollmentStatsPort;
import com.wanted.backend.domain.cource.application.port.InstructorQueryPort;
import com.wanted.backend.domain.cource.application.port.InstructorStatsPort;
import com.wanted.backend.domain.cource.application.port.ReviewStatsPort;
import com.wanted.backend.domain.cource.application.query.CourseListQuery;
import com.wanted.backend.domain.cource.application.usecase.CourseQueryUseCase;
import com.wanted.backend.domain.cource.domain.model.Course;
import com.wanted.backend.domain.cource.domain.model.CourseListItem;
import com.wanted.backend.domain.cource.domain.model.CourseSortType;
import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.PageResult;
import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.global.config.S3UrlPresigner;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService implements CourseQueryUseCase {

    // 강사 대시보드 "퀴즈 수" 카드 - quiz 도메인이 Mock API라 임시 고정값 사용
    private static final int MOCK_QUIZ_COUNT = 36;

    private final CourseRepository courseRepository;
    private final InstructorQueryPort instructorQueryPort;
    private final ReviewStatsPort reviewStatsPort;
    private final EnrollmentStatsPort enrollmentStatsPort;
    private final InstructorStatsPort instructorStatsPort;
    private final CourseLearningPolicyPort courseLearningPolicyPort;
    private final S3UrlPresigner s3UrlPresigner;

    @Override
    public CourseListResult getList(CourseListQuery query) {
        List<Long> authorIds = null;
        if (query.instructorName() != null && !query.instructorName().isBlank()) {
            authorIds = instructorQueryPort.findIdsByName(query.instructorName());
            if (authorIds.isEmpty()) {
                return new CourseListResult(Collections.emptyList(), query.page(), 0, 0);
            }
        }

        boolean needsInMemorySort = query.sort() == CourseSortType.POPULAR
                || query.sort() == CourseSortType.RATING;

        if (needsInMemorySort) {
            return getListWithInMemorySort(query, authorIds);
        }

        PageResult<CourseListItem> pageResult = courseRepository.findList(
                query.keyword(), query.subject(), authorIds, query.sort(), query.page(), query.size());

        return toResult(pageResult.content(), pageResult.currentPage(),
                pageResult.totalPages(), pageResult.totalCount(), query.page(), query.size());
    }

    private CourseListResult getListWithInMemorySort(CourseListQuery query, List<Long> authorIds) {
        // 전체 조회 후 집계값 기반 정렬 — 페이지 단위 정렬은 전체 순위를 보장하지 못함
        List<CourseListItem> all = courseRepository.findAllForSort(
                query.keyword(), query.subject(), authorIds);

        if (all.isEmpty()) {
            return new CourseListResult(Collections.emptyList(), query.page(), 0, 0);
        }

        List<Long> allCourseIds = all.stream().map(CourseListItem::courseId).toList();
        Map<Long, ReviewStatsPort.Stats> reviewStatsMap = reviewStatsPort.findStatsByCourseIds(allCourseIds);
        Map<Long, Integer> enrollmentCountMap = enrollmentStatsPort.countByCourseIds(allCourseIds);

        List<Long> authorIdList = all.stream().map(CourseListItem::authorId).distinct().toList();
        Map<Long, String> nameMap = authorIdList.isEmpty() ? Map.of() : instructorQueryPort.findNamesByIds(authorIdList);

        List<CourseListResult.Item> items = all.stream()
                .map(item -> {
                    ReviewStatsPort.Stats stats = reviewStatsMap.getOrDefault(item.courseId(), ReviewStatsPort.Stats.EMPTY);
                    return new CourseListResult.Item(
                            item.courseId(), item.title(), item.subject(), s3UrlPresigner.publicUrl(item.thumbnailUrl()),
                            item.priceType(), item.price(),
                            nameMap.getOrDefault(item.authorId(), "알 수 없음"),
                            stats.avgRating(), stats.reviewCount(),
                            enrollmentCountMap.getOrDefault(item.courseId(), 0),
                            item.status(), item.createdAt()
                    );
                })
                .sorted(query.sort() == CourseSortType.POPULAR
                        ? Comparator.comparingInt(CourseListResult.Item::studentCount).reversed()
                        : Comparator.comparingDouble(CourseListResult.Item::rating).reversed())
                .collect(Collectors.toList());

        int total = items.size();
        int size = query.size() > 0 ? query.size() : 10;
        int totalPages = (int) Math.ceil((double) total / size);
        int page = Math.max(0, Math.min(query.page(), totalPages - 1));
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<CourseListResult.Item> pageItems = fromIndex < total ? items.subList(fromIndex, toIndex) : Collections.emptyList();

        return new CourseListResult(pageItems, page, totalPages, total);
    }

    private CourseListResult toResult(List<CourseListItem> content, int currentPage,
                                      int totalPages, long totalCount,
                                      int requestedPage, int requestedSize) {
        if (content.isEmpty()) {
            return new CourseListResult(Collections.emptyList(), currentPage, totalPages, (int) totalCount);
        }
        List<Long> ids = content.stream().map(CourseListItem::authorId).distinct().toList();
        Map<Long, String> nameMap = ids.isEmpty() ? Map.of() : instructorQueryPort.findNamesByIds(ids);

        List<Long> courseIds = content.stream().map(CourseListItem::courseId).toList();
        Map<Long, ReviewStatsPort.Stats> reviewStatsMap = reviewStatsPort.findStatsByCourseIds(courseIds);
        Map<Long, Integer> enrollmentCountMap = enrollmentStatsPort.countByCourseIds(courseIds);

        List<CourseListResult.Item> items = content.stream()
                .map(item -> {
                    ReviewStatsPort.Stats stats = reviewStatsMap.getOrDefault(item.courseId(), ReviewStatsPort.Stats.EMPTY);
                    return new CourseListResult.Item(
                            item.courseId(), item.title(), item.subject(), s3UrlPresigner.publicUrl(item.thumbnailUrl()),
                            item.priceType(), item.price(),
                            nameMap.getOrDefault(item.authorId(), "알 수 없음"),
                            stats.avgRating(), stats.reviewCount(),
                            enrollmentCountMap.getOrDefault(item.courseId(), 0),
                            item.status(), item.createdAt()
                    );
                })
                .collect(Collectors.toList());

        return new CourseListResult(items, currentPage, totalPages, (int) totalCount);
    }

    @Override
    public CourseDetailResult getDetail(Long courseId, Long requesterId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            if (requesterId == null || !requesterId.equals(course.getAuthorId())) {
                throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
            }
        }

        Map<Long, String> nameMap = instructorQueryPort.findNamesByIds(List.of(course.getAuthorId()));
        String instructorName = nameMap.getOrDefault(course.getAuthorId(), "알 수 없음");
        InstructorQueryPort.InstructorProfile instructorProfile = instructorQueryPort.findProfileById(course.getAuthorId());

        // 학습 정책(권장 완강 기간·하루 강도 상한). 정책 레코드가 없는 구 강의는 두 값 모두 null.
        CourseLearningPolicyPort.LearningPolicy policy = courseLearningPolicyPort.find(courseId).orElse(null);
        Integer recommendedWeeks = policy == null ? null : policy.recommendedWeeks();
        Integer dailyMaxMinutes = policy == null ? null : policy.dailyMaxMinutes();

        List<CourseDetailResult.SectionResult> sections = course.getSections().stream()
                .sorted(Comparator.comparingInt(s -> s.getOrderIndex()))
                .map(section -> new CourseDetailResult.SectionResult(
                        section.getId(),
                        section.getTitle(),
                        section.getOrderIndex(),
                        section.getLessons().stream()
                                .sorted(Comparator.comparingInt(l -> l.getOrderIndex()))
                                .map(lesson -> new CourseDetailResult.LessonResult(
                                        lesson.getId(),
                                        lesson.getTitle(),
                                        lesson.getDescription(),
                                        lesson.getOrderIndex(),
                                        lesson.getDurationSeconds(),
                                        section.getOrderIndex() == 0 && lesson.getOrderIndex() == 0
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new CourseDetailResult(
                course.getId(), course.getTitle(), course.getSubject(),
                course.getDescription(), s3UrlPresigner.publicUrl(course.getThumbnailUrl()),
                course.getPriceType(), course.getPrice(), course.getStatus(),
                instructorName,
                reviewStatsPort.avgRating(course.getId()),
                reviewStatsPort.reviewCount(course.getId()),
                enrollmentStatsPort.enrollmentCount(course.getId()),
                instructorStatsPort.totalStudents(course.getAuthorId()),
                instructorStatsPort.totalCourses(course.getAuthorId()),
                instructorStatsPort.avgRating(course.getAuthorId()),
                instructorProfile.oneLineIntro(),
                instructorProfile.introduction(),
                instructorProfile.career(),
                sections,
                course.getLearningObjectives(), course.getTargetAudience(),
                course.getTechTags(), course.getLevel(),
                recommendedWeeks, dailyMaxMinutes
        );
    }

    @Override
    public CourseListResult getInstructorCourses(Long instructorId, int page, int size) {
        PageResult<CourseListItem> pageResult = courseRepository.findByAuthor(instructorId, page, size);
        return toResult(pageResult.content(), pageResult.currentPage(),
                pageResult.totalPages(), pageResult.totalCount(), page, size);
    }

    @Override
    public InstructorDashboardResult getInstructorDashboard(Long instructorId) {
        InstructorStatsPort.CourseCounts counts = instructorStatsPort.courseCounts(instructorId);
        int totalStudents = instructorStatsPort.totalStudents(instructorId);

        return new InstructorDashboardResult(
                counts.total(), counts.published(), counts.hidden(),
                totalStudents, MOCK_QUIZ_COUNT
        );
    }
}
