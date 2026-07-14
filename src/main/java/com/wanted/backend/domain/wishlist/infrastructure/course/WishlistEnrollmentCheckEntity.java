package com.wanted.backend.domain.wishlist.infrastructure.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Immutable
@Entity(name = "WishlistEnrollment")
@Getter
@Table(name = "enrollment")
public class WishlistEnrollmentCheckEntity {

    @Id
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "status")
    private String status;
}
