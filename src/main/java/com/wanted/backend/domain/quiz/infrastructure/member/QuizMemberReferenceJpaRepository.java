package com.wanted.backend.domain.quiz.infrastructure.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizMemberReferenceJpaRepository extends JpaRepository<QuizMemberReferenceJpaEntity, Long> {
}
