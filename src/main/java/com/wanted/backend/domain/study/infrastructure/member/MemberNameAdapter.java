package com.wanted.backend.domain.study.infrastructure.member;

import com.wanted.backend.domain.study.application.port.MemberNamePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Transactional(readOnly = true)
public class MemberNameAdapter implements MemberNamePort {

    private final SpringDataMemberReferenceRepository memberReferenceRepository;

    public MemberNameAdapter(SpringDataMemberReferenceRepository memberReferenceRepository) {
        this.memberReferenceRepository = memberReferenceRepository;
    }

    @Override
    public Map<Long, String> getNamesByMemberIds(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> namesByMemberId = new HashMap<>();
        memberReferenceRepository.findByIdIn(memberIds)
                .forEach(entity -> namesByMemberId.put(entity.getId(), entity.getName()));
        return namesByMemberId;
    }
}
