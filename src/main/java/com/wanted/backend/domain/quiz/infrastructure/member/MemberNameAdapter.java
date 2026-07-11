package com.wanted.backend.domain.quiz.infrastructure.member;

import com.wanted.backend.domain.quiz.application.port.MemberNamePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberNameAdapter implements MemberNamePort {

    private final QuizMemberReferenceJpaRepository memberRepository;

    @Override
    public Map<Long, String> findNamesByIds(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(
                        QuizMemberReferenceJpaEntity::getId,
                        QuizMemberReferenceJpaEntity::getName,
                        (a, b) -> a));
    }
}
