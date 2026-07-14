package com.wanted.backend.domain.cart.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CartCourseJpaRepository extends JpaRepository<CartCourseJpaEntity, Long> {

    // 카탈로그에서 내려간(DELETED/DRAFT) 강의는 장바구니 상세/결제에서 제외 — PUBLISHED만 노출한다.
    @Query("SELECT c FROM CartCourse c WHERE c.id IN :ids AND c.status = 'PUBLISHED'")
    List<CartCourseJpaEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);
}
