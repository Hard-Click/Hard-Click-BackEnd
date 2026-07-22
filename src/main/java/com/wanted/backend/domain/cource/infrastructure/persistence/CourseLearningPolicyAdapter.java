package com.wanted.backend.domain.cource.infrastructure.persistence;

import com.wanted.backend.domain.cource.application.port.CourseLearningPolicyPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * course_learning_policy 저장 어댑터. 전용 JPA 엔티티 없이 native upsert 로 처리한다
 * (해당 테이블은 CP-SAT 입력 전용이라 도메인 애그리거트에 얹지 않고 정책 레코드만 관리).
 * 매핑: recommendedWeeks -> recommended_duration_weeks(V3.1.1), dailyMaxMinutes -> daily_recommended_minutes(V2).
 */
@Component
@RequiredArgsConstructor
public class CourseLearningPolicyAdapter implements CourseLearningPolicyPort {

    private final EntityManager entityManager;

    @Override
    public void save(Long courseId, Integer recommendedWeeks, Integer dailyMaxMinutes) {
        // course_id 는 PK(1:1). 강의 수정 시 재호출될 수 있어 upsert(중복 시 갱신)로 둔다.
        // VALUES() 는 MySQL 8.0.20 에서 deprecated 됐으므로 바인딩 파라미터를 직접 참조한다(버전 무관 안전).
        entityManager.createNativeQuery("""
                insert into course_learning_policy (course_id, recommended_duration_weeks, daily_recommended_minutes)
                values (:courseId, :weeks, :daily)
                on duplicate key update
                    recommended_duration_weeks = :weeks,
                    daily_recommended_minutes  = :daily
                """)
                .setParameter("courseId", courseId)
                .setParameter("weeks", recommendedWeeks)
                .setParameter("daily", dailyMaxMinutes)
                .executeUpdate();
    }

    @Override
    public Optional<LearningPolicy> find(Long courseId) {
        // course_id 는 PK(1:1)라 0 또는 1행. 값 컬럼은 nullable이므로 Number 캐스팅 시 null 방어.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select recommended_duration_weeks, daily_recommended_minutes
                from course_learning_policy
                where course_id = :courseId
                """)
                .setParameter("courseId", courseId)
                .getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        Integer weeks = row[0] == null ? null : ((Number) row[0]).intValue();
        Integer daily = row[1] == null ? null : ((Number) row[1]).intValue();
        return Optional.of(new LearningPolicy(weeks, daily));
    }
}
