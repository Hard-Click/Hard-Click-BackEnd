package com.wanted.backend.domain.cart.infrastructure.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

// 카탈로그에서 내려간(DELETED/DRAFT) 강의는 장바구니 상세/결제에서 제외 — PUBLISHED만 노출.
// status(enum) 컬럼을 엔티티 필드로 매핑하면 Hibernate validate가 enum↔varchar로 실패하므로
// SQL 레벨 필터(@SQLRestriction)로 처리한다.
@Entity(name = "CartCourse")
@Getter
@Immutable
@SQLRestriction("status = 'PUBLISHED'")
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartCourseJpaEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "author_id", nullable = false)
    private Long authorId;
}
