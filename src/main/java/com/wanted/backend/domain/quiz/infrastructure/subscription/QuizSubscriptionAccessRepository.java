package com.wanted.backend.domain.quiz.infrastructure.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface QuizSubscriptionAccessRepository
        extends JpaRepository<QuizSubscriptionReferenceJpaEntity, Long> {

    boolean existsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(
            Long memberId,
            QuizSubscriptionStatus status,
            LocalDateTime now
    );
}
