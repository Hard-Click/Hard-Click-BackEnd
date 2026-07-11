package com.wanted.backend.domain.quiz.infrastructure.member;

import com.wanted.backend.domain.quiz.application.port.MemberNamePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 클래스명은 도메인별로 고유해야 한다 — community의 MemberNameAdapter와 빈 이름이 겹치면
// ConflictingBeanDefinitionException으로 부팅이 실패한다(참조 엔티티 명명 규칙과 동일).
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizMemberNameAdapter implements MemberNamePort {

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
