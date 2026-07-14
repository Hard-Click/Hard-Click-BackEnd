package com.wanted.backend.domain.quiz.application.port;

import java.util.Collection;
import java.util.Map;

/**
 * 회원 id → 표시 이름 배치 조회 아웃바운드 포트 (강사명 표시 등).
 */
public interface MemberNamePort {

    Map<Long, String> findNamesByIds(Collection<Long> memberIds);
}
