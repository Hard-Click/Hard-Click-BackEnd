package com.wanted.backend.domain.quiz.infrastructure.cource;

import com.wanted.backend.domain.quiz.application.port.AdminCourseListPort;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCourseListAdapter implements AdminCourseListPort {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DELETED = "DELETED";

    private final CourseReferenceJpaRepository courseRepository;

    @Override
    public List<AdminCourse> findCourses(String subject, Long instructorId, Long courseId, String keyword) {
        Specification<CourseReferenceJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // soft-delete된 강의는 관리 목록에서 제외 (DRAFT/PUBLISHED만)
            predicates.add(cb.notEqual(root.get("status"), STATUS_DELETED));
            if (subject != null && !subject.isBlank()) {
                predicates.add(cb.equal(root.get("subject"), subject.strip()));
            }
            if (instructorId != null) {
                predicates.add(cb.equal(root.get("authorId"), instructorId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("id"), courseId));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + keyword.strip().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return courseRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(c -> new AdminCourse(
                        c.getId(),
                        c.getTitle(),
                        STATUS_PUBLISHED.equals(c.getStatus()),
                        c.getAuthorId(),
                        c.getCreatedAt()))
                .toList();
    }
}
