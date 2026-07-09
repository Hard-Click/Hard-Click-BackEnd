package com.wanted.backend.domain.chat.application.port;

import java.util.Collection;
import java.util.Map;

public interface MemberNamePort {
    Map<Long, String> getNamesByMemberIds(Collection<Long> memberIds);
}
