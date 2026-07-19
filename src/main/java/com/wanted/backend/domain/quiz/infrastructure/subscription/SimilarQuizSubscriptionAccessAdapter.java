package com.wanted.backend.domain.quiz.infrastructure.subscription;

import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimilarQuizSubscriptionAccessAdapter implements SimilarQuizSubscriptionAccessPort {

    private final QuizSubscriptionAccessRepository repository;

    @Override
    public boolean hasActiveSubscription(Long memberId) {
        return repository.existsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(
                memberId,
                QuizSubscriptionStatus.ACTIVE,
                LocalDateTime.now()
        );
    }
}
