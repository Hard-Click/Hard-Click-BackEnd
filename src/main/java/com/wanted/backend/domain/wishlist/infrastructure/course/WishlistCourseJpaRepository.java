package com.wanted.backend.domain.wishlist.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistCourseJpaRepository extends JpaRepository<WishlistCourseJpaEntity, Long> {
    // 카탈로그에서 내려간(DELETED/DRAFT) 강의는 찜 상세에서 제외 — PUBLISHED만 노출한다.
    @Query("SELECT c FROM WishlistCourse c WHERE c.id IN :ids AND c.status = 'PUBLISHED'")
    List<WishlistCourseJpaEntity> findAllByIdIn(@Param("ids") List<Long> ids);
}
