package com.wanted.backend.domain.wishlist.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistEnrollmentCheckRepository extends JpaRepository<WishlistEnrollmentCheckEntity, Long> {

    // ACTIVE(IN_PROGRESS/COMPLETED) 상태만 "수강중"으로 본다. REFUNDED/EXPIRED/ENROLLED 제외.
    // (learning_activity 접근 정책과 동일 기준 — 환불 강의가 찜 카드에서 "학습하기"로 뜨는 문제 방지)
    @Query("SELECT e.courseId FROM WishlistEnrollment e WHERE e.memberId = :memberId AND e.courseId IN :courseIds AND e.status IN ('IN_PROGRESS', 'COMPLETED')")
    List<Long> findEnrolledCourseIds(@Param("memberId") Long memberId, @Param("courseIds") List<Long> courseIds);

    @Query("SELECT e.courseId, COUNT(e.id) FROM WishlistEnrollment e WHERE e.courseId IN :courseIds GROUP BY e.courseId")
    List<Object[]> countEnrollmentsByCourseIds(@Param("courseIds") List<Long> courseIds);
}
