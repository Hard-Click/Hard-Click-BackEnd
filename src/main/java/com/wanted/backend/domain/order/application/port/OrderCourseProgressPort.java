package com.wanted.backend.domain.order.application.port;

import java.util.Collection;
import java.util.Map;

/**
 * 환불 정책(진도율 10% 미만) 판정을 위해 order 컨텍스트가 강의 진도율을 조회하는 포트.
 * 진도율 정의는 "완료 레슨 수 / 전체 레슨 수" (미리보기 레슨 포함).
 */
public interface OrderCourseProgressPort {

    /**
     * courseId -> 진도율(%, 0~100 정수). 진도 기록이 없거나 레슨이 없으면 0.
     */
    Map<Long, Integer> findProgressPercents(Long memberId, Collection<Long> courseIds);
}
