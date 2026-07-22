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
    @Column(name = "member_id", insertable = false, updatable = false)
    private Long id;

    // members.role은 enum 컬럼이라, String 매핑 시 ddl-auto=validate가 타입 불일치(enum↔varchar)로 실패한다.
    // 읽기 전용(insertable=false, updatable=false)으로 둬 타입 검증을 우회한다(notice MemberReferenceEntity와 동일).
    @Column(name = "role", insertable = false, updatable = false)
    private String role;
}
