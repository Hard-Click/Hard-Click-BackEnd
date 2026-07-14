package com.wanted.backend.domain.wishlist.infrastructure.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Immutable
@Entity(name = "WishlistCourse")
@Getter
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

    @Column(name = "status")
    private String status;
}
