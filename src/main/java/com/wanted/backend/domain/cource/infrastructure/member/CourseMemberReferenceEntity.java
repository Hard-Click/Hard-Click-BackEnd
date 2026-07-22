package com.wanted.backend.domain.cource.infrastructure.member;

import com.wanted.backend.domain.identity.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // members.role은 enum 컬럼. 같은 members 테이블을 매핑하는 엔티티들은 하나의 Table 모델에 role 컬럼을
    // 공유하므로, canonical MemberJpaEntity와 동일하게 Role enum + @Enumerated(STRING)으로 매핑해야
    // ddl-auto=validate가 enum↔varchar 불일치 없이 통과한다(String 매핑 시 varchar로 인식돼 실패).
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;
}
