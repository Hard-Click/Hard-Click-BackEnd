package com.wanted.backend.domain.wishlist.infrastructure.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

// 카탈로그에서 내려간(DELETED/DRAFT) 강의는 찜 상세에서 제외 — PUBLISHED만 노출.
// status(enum) 컬럼을 필드로 매핑하면 Hibernate validate가 enum↔varchar로 실패하므로 SQL 레벨 필터로 처리.
@Immutable
@Entity(name = "WishlistCourse")
@Getter
@SQLRestriction("status = 'PUBLISHED'")
@Table(name = "course")
public class WishlistCourseJpaEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "subject")
    private String subject;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price_type")
    private String priceType;

    @Column(name = "price")
    private Integer price;

    @Column(name = "author_id")
    private Long authorId;
}
