package com.wanted.backend.domain.cart.infrastructure.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "CartCourse")
@Getter
@Immutable
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

    @Column(name = "status")
    private String status;
}
