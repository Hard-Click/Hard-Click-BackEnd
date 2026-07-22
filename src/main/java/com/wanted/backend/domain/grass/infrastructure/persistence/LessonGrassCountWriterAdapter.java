package com.wanted.backend.domain.grass.infrastructure.persistence;

import com.wanted.backend.domain.grass.application.port.LessonGrassCountWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class LessonGrassCountWriterAdapter implements LessonGrassCountWriter {

    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    @Transactional
    public void increment(Long memberId, LocalDate statDate) {
        // (member_id, stat_date) 유니크 키 위에서 원자적 증가. 앱 락 불필요(DB 행 단위 원자 연산).
        // 같은 행의 study_seconds 집계(study_timer)와 다른 컬럼이라 상호 간섭 없음.
        LocalDateTime now = LocalDateTime.now(clock);
        entityManager.createNativeQuery("""
                INSERT INTO daily_study_stats
                    (member_id, stat_date, watched_lesson_count, study_seconds, completed_lesson_count, created_at, updated_at)
                VALUES
                    (:memberId, :statDate, 1, 0, 0, :now, :now)
                ON DUPLICATE KEY UPDATE
                    watched_lesson_count = watched_lesson_count + 1,
                    updated_at = :now
                """)
                .setParameter("memberId", memberId)
                .setParameter("statDate", statDate)
                .setParameter("now", now)
                .executeUpdate();
    }
}
