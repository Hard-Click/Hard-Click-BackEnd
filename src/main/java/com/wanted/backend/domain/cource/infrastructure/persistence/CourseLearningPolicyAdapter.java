package com.wanted.backend.domain.cource.infrastructure.persistence;

import com.wanted.backend.domain.cource.application.port.CourseLearningPolicyPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
