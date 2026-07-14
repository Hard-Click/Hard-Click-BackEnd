package com.wanted.backend.domain.wishlist.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistCourseJpaRepository extends JpaRepository<WishlistCourseJpaEntity, Long> {
    // PUBLISHED 필터는 WishlistCourseJpaEntity의 @SQLRestriction으로 자동 적용된다.
    List<WishlistCourseJpaEntity> findAllByIdIn(List<Long> ids);
}
