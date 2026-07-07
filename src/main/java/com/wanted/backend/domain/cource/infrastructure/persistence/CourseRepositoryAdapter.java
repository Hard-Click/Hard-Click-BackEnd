package com.wanted.backend.domain.cource.infrastructure.persistence;

import com.wanted.backend.domain.cource.domain.model.*;
import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseRepositoryAdapter implements CourseRepository {

    private final SpringDataCourseRepository jpaRepository;
    private final Clock clock;

    @Override
    public Course save(Course course) {
        if (course.getId() == null) {
            CourseJpaEntity entity = CourseJpaEntity.from(
                    course.getAuthorId(), course.getTitle(), course.getSubject(),
                    course.getDescription(), course.getThumbnailUrl(),
                    course.getPriceType(), course.getPrice(), course.getStatus(), course.getCreatedAt(),
                    course.getLearningObjectives(), course.getTargetAudience(),
                    course.getTechTags(), course.getLevel());
            CourseJpaEntity saved = jpaRepository.saveAndFlush(entity);
            course.getSections().forEach(section -> {
                CourseSectionJpaEntity sectionEntity = saved.addSection(
                        section.getTitle(), section.getOrderIndex());
                section.getLessons().forEach(lesson ->
                        sectionEntity.addLesson(lesson.getTitle(), lesson.getDescription(),
                                lesson.getOrderIndex(), lesson.getDurationSeconds(), course.getCreatedAt()));
            });
            return toDomain(jpaRepository.save(saved));
        }
        // 수정: 기존 엔티티 로드 후 필드 동기화
        CourseJpaEntity entity = jpaRepository.findById(course.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        Hibernate.initialize(entity.getSections());
        entity.getSections().forEach(s -> Hibernate.initialize(s.getLessons()));

        entity.update(course.getTitle(), course.getSubject(), course.getDescription(),
                course.getThumbnailUrl(), course.getPriceType(), course.getPrice(), course.getStatus(),
                course.getLearningObjectives(), course.getTargetAudience(),
                course.getTechTags(), course.getLevel());

        syncSections(entity, course.getSections());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Course> findById(Long courseId) {
        return jpaRepository.findById(courseId).map(this::toDomain);
    }

    @Override
    public void delete(Long courseId) {
        jpaRepository.deleteById(courseId);
    }

    @Override
    public PageResult<CourseListItem> findList(String keyword, String subject, List<Long> authorIds,
                                               CourseSortType sort, int page, int size) {
        Specification<CourseJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 공개 강의만 노출
            predicates.add(cb.equal(root.get("status"), CourseStatus.PUBLISHED));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
            }
            if (subject != null && !subject.isBlank()) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }
            if (authorIds != null && !authorIds.isEmpty()) {
                predicates.add(root.get("authorId").in(authorIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortOrder = switch (sort) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            // POPULAR/RATING은 추후 연동 전까지 최신순으로 대체
            case POPULAR, RATING -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Page<CourseJpaEntity> result = jpaRepository.findAll(spec, PageRequest.of(page, size, sortOrder));

        List<CourseListItem> items = result.getContent().stream()
                .map(e -> new CourseListItem(
                        e.getId(), e.getAuthorId(), e.getTitle(), e.getSubject(),
                        e.getThumbnailUrl(), e.getPriceType(), e.getPrice(),
                        e.getStatus(), e.getCreatedAt()))
                .toList();

        return new PageResult<>(items, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Override
    public List<CourseListItem> findAllForSort(String keyword, String subject, List<Long> authorIds) {
        Specification<CourseJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), CourseStatus.PUBLISHED));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
            }
            if (subject != null && !subject.isBlank()) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }
            if (authorIds != null && !authorIds.isEmpty()) {
                predicates.add(root.get("authorId").in(authorIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return jpaRepository.findAll(spec).stream()
                .map(e -> new CourseListItem(
                        e.getId(), e.getAuthorId(), e.getTitle(), e.getSubject(),
                        e.getThumbnailUrl(), e.getPriceType(), e.getPrice(),
                        e.getStatus(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public PageResult<CourseListItem> findByAuthor(Long authorId, int page, int size) {
        Specification<CourseJpaEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("authorId"), authorId),
                cb.notEqual(root.get("status"), CourseStatus.DELETED));

        Page<CourseJpaEntity> result = jpaRepository.findAll(
                spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<CourseListItem> items = result.getContent().stream()
                .map(e -> new CourseListItem(
                        e.getId(), e.getAuthorId(), e.getTitle(), e.getSubject(),
                        e.getThumbnailUrl(), e.getPriceType(), e.getPrice(),
                        e.getStatus(), e.getCreatedAt()))
                .toList();

        return new PageResult<>(items, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    // ── 섹션 동기화 ──────────────────────────────────────────────────────────────
    private void syncSections(CourseJpaEntity entity, List<CourseSection> domainSections) {
        // Must Fix 3: 요청 섹션 ID가 현재 강의 소속인지 검증 (조용한 무시 방지)
        Set<Long> existingSectionIds = entity.getSections().stream()
                .map(CourseSectionJpaEntity::getId)
                .collect(Collectors.toSet());
        for (CourseSection ds : domainSections) {
            if (ds.getId() != null && !existingSectionIds.contains(ds.getId())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 요청에 없는 기존 섹션 제거 (orphanRemoval이 DB 삭제 처리)
        Set<Long> keepIds = domainSections.stream()
                .filter(s -> s.getId() != null)
                .map(CourseSection::getId)
                .collect(Collectors.toSet());
        entity.getSections().removeIf(s -> !keepIds.contains(s.getId()));

        for (CourseSection domainSection : domainSections) {
            if (domainSection.getId() != null) {
                // 기존 섹션: 제목/순서 업데이트 후 회차 동기화
                entity.getSections().stream()
                        .filter(s -> s.getId().equals(domainSection.getId()))
                        .findFirst()
                        .ifPresent(s -> {
                            s.update(domainSection.getTitle(), domainSection.getOrderIndex());
                            syncLessons(s, domainSection.getLessons());
                        });
            } else {
                // 신규 섹션: 회차까지 새로 생성
                CourseSectionJpaEntity newSection = entity.addSection(
                        domainSection.getTitle(), domainSection.getOrderIndex());
                domainSection.getLessons().forEach(l ->
                        newSection.addLesson(l.getTitle(), l.getDescription(),
                                l.getOrderIndex(), l.getDurationSeconds(), clock.instant()));
            }
        }
    }

    // ── 회차 동기화 ──────────────────────────────────────────────────────────────
    private void syncLessons(CourseSectionJpaEntity sectionEntity, List<Lesson> domainLessons) {
        // Must Fix 3: 요청 회차 ID가 현재 섹션 소속인지 검증 (조용한 무시 방지)
        Set<Long> existingLessonIds = sectionEntity.getLessons().stream()
                .map(LessonJpaEntity::getId)
                .collect(Collectors.toSet());
        for (Lesson dl : domainLessons) {
            if (dl.getId() != null && !existingLessonIds.contains(dl.getId())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Set<Long> keepIds = domainLessons.stream()
                .filter(l -> l.getId() != null)
                .map(Lesson::getId)
                .collect(Collectors.toSet());
        sectionEntity.getLessons().removeIf(l -> !keepIds.contains(l.getId()));

        for (Lesson domainLesson : domainLessons) {
            if (domainLesson.getId() != null) {
                // 기존 회차: 제목/설명/순서만 업데이트, videoUrl 등은 보존
                sectionEntity.getLessons().stream()
                        .filter(l -> l.getId().equals(domainLesson.getId()))
                        .findFirst()
                        .ifPresent(l -> l.updateMeta(domainLesson.getTitle(),
                                domainLesson.getDescription(), domainLesson.getOrderIndex()));
            } else {
                // 신규 회차
                sectionEntity.addLesson(domainLesson.getTitle(), domainLesson.getDescription(),
                        domainLesson.getOrderIndex(), domainLesson.getDurationSeconds(), clock.instant());
            }
        }
    }

    // ── 신규 강의 JPA 엔티티 생성 ────────────────────────────────────────────────
    private CourseJpaEntity toNewJpaEntity(Course course) {
        CourseJpaEntity entity = CourseJpaEntity.from(
                course.getAuthorId(), course.getTitle(), course.getSubject(),
                course.getDescription(), course.getThumbnailUrl(),
                course.getPriceType(), course.getPrice(), course.getStatus(), course.getCreatedAt(),
                course.getLearningObjectives(), course.getTargetAudience(),
                course.getTechTags(), course.getLevel());
        course.getSections().forEach(section -> {
            CourseSectionJpaEntity sectionEntity = entity.addSection(
                    section.getTitle(), section.getOrderIndex());
            section.getLessons().forEach(lesson ->
                    sectionEntity.addLesson(lesson.getTitle(), lesson.getDescription(),
                            lesson.getOrderIndex(), lesson.getDurationSeconds(), course.getCreatedAt()));
        });
        return entity;
    }

    // ── JPA 엔티티 → 도메인 객체 ─────────────────────────────────────────────────
    private Course toDomain(CourseJpaEntity entity) {
        Hibernate.initialize(entity.getSections());

        List<CourseSection> sections = entity.getSections().stream()
                .map(sectionEntity -> {
                    Hibernate.initialize(sectionEntity.getLessons());
                    List<Lesson> lessons = sectionEntity.getLessons().stream()
                            .map(l -> Lesson.restore(
                                    l.getId(), sectionEntity.getId(),
                                    l.getTitle(), l.getDescription(), l.getOrderIndex(),
                                    l.getVideoUrl(), l.getS3Key(), l.getDurationSeconds(),
                                    l.getFileProcessingStatus(), l.getCreatedAt()))
                            .toList();
                    return CourseSection.restore(
                            sectionEntity.getId(), sectionEntity.getTitle(),
                            sectionEntity.getOrderIndex(), lessons);
                })
                .toList();

        return Course.restore(
                entity.getId(), entity.getAuthorId(), entity.getTitle(), entity.getSubject(),
                entity.getDescription(), entity.getThumbnailUrl(), entity.getPriceType(),
                entity.getPrice(), entity.getStatus(), sections, entity.getCreatedAt(),
                entity.getLearningObjectivesList(), entity.getTargetAudienceList(),
                entity.getTechTagsList(), entity.getLevel());
    }
}
