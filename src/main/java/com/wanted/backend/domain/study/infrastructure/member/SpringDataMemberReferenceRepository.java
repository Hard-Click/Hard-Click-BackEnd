package com.wanted.backend.domain.study.infrastructure.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpringDataMemberReferenceRepository extends JpaRepository<MemberReferenceEntity, Long> {
    List<MemberReferenceEntity> findByIdIn(Collection<Long> ids);
}
