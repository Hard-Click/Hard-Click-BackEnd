package com.wanted.backend.domain.cource.infrastructure.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 관리자 여부 판별용 members 테이블 읽기 전용 참조(역할 컬럼만).
 * 도메인별 엔티티명 충돌을 피하려 @Entity name을 CourseMemberReference로 둔다.
 */
@Entity(name = "CourseMemberReference")
@Table(name = "members")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseMemberReferenceEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @Column(name = "role")
    private String role;
}
