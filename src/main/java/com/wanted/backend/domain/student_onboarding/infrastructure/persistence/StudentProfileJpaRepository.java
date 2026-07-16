package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileJpaEntity, Long> {
}
