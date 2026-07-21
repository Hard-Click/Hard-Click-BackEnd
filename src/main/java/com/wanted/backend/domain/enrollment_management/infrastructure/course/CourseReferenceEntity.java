package com.wanted.backend.domain.enrollment_management.infrastructure.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "EnrollmentCourseReference")
@Getter
@Immutable
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReferenceEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // course.status enum('DELETED','DRAFT','PUBLISHED'). 소프트 삭제는 status=DELETED 로 표현된다
    // (course 테이블엔 deleted_at 컬럼이 없음). 수강 목록에서 삭제 강의를 거르는 데 쓴다.
    @Column(name = "status", nullable = false)
    private String status;
}
