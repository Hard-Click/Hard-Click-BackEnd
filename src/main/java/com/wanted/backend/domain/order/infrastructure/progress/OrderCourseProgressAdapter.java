package com.wanted.backend.domain.order.infrastructure.progress;

import com.wanted.backend.domain.order.application.port.OrderCourseProgressPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 진도율(%) = 완료 레슨 수 / 전체 레슨 수 (미리보기 레슨 포함).
 * 크로스 컨텍스트 규칙에 따라 다른 도메인 엔티티를 참조하지 않고 native SQL로 계산한다.
 * 완료 집계는 현재 존재하는 레슨(lesson JOIN)으로 한정해 삭제된 레슨의 잔여 진도가 섞이지 않게 한다.
 */
@Component
public class OrderCourseProgressAdapter implements OrderCourseProgressPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> findProgressPercents(Long memberId, Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> totalByCourse = toCountMap(em.createNativeQuery(
                        "SELECT s.course_id, COUNT(*) " +
                                "FROM lesson l JOIN course_section s ON l.section_id = s.id " +
                                "WHERE s.course_id IN (:courseIds) " +
                                "GROUP BY s.course_id")
                .setParameter("courseIds", courseIds)
                .getResultList());

        Map<Long, Long> completedByCourse = toCountMap(em.createNativeQuery(
                        "SELECT vp.course_id, COUNT(*) " +
                                "FROM video_progress vp " +
                                "JOIN lesson l ON l.id = vp.video_id " +
                                "WHERE vp.member_id = :memberId AND vp.course_id IN (:courseIds) " +
                                "AND vp.is_completed = 1 " +
                                "GROUP BY vp.course_id")
                .setParameter("memberId", memberId)
                .setParameter("courseIds", courseIds)
                .getResultList());

        Map<Long, Integer> result = new HashMap<>();
        for (Long courseId : courseIds) {
            long total = totalByCourse.getOrDefault(courseId, 0L);
            long completed = completedByCourse.getOrDefault(courseId, 0L);
            int percent = total == 0 ? 0 : (int) (completed * 100 / total);
            result.put(courseId, percent);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> toCountMap(List<?> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object row : (List<Object[]>) rows) {
            Object[] cols = (Object[]) row;
            map.put(((Number) cols[0]).longValue(), ((Number) cols[1]).longValue());
        }
        return map;
    }
}
