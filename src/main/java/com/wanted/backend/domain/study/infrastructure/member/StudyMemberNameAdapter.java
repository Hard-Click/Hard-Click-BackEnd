package com.wanted.backend.domain.study.infrastructure.member;

import com.wanted.backend.domain.study.application.port.MemberNamePort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class StudyMemberNameAdapter implements MemberNamePort {

    private final com.wanted.backend.domain.community.application.port.MemberNamePort communityMemberNamePort;

    public StudyMemberNameAdapter(com.wanted.backend.domain.community.application.port.MemberNamePort communityMemberNamePort) {
        this.communityMemberNamePort = communityMemberNamePort;
    }

    @Override
    public Map<Long, String> getNamesByMemberIds(Collection<Long> memberIds) {
        return communityMemberNamePort.getNamesByMemberIds(memberIds);
    }
}
