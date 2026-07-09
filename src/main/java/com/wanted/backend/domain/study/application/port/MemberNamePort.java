package com.wanted.backend.domain.study.application.port;

import java.util.Collection;
import java.util.Map;

public interface MemberNamePort {
    Map<Long, String> getNamesByMemberIds(Collection<Long> memberIds);
}
