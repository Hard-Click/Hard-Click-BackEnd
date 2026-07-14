package com.wanted.backend.domain.cart.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CartCourseJpaRepository extends JpaRepository<CartCourseJpaEntity, Long> {

    // PUBLISHED 필터는 CartCourseJpaEntity의 @SQLRestriction으로 자동 적용된다.
    @Query("SELECT c FROM CartCourse c WHERE c.id IN :ids")
    List<CartCourseJpaEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);
}
