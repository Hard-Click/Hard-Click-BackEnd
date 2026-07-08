package com.wanted.backend.domain.quiz.infrastructure.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// members 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴) — 수강생 아이디/이름 표시용.
// 클래스명은 도메인별 고유(Hibernate 엔티티명 충돌 방지 — 타 도메인 MemberReferenceEntity와 구분).
@Entity
@Immutable
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizMemberReferenceJpaEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;
}
