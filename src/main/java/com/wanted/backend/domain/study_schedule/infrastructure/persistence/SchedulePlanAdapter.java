package com.wanted.backend.domain.study_schedule.infrastructure.persistence;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * schedule_slot(+weekly_schedule/enrollment/course/lesson) native read/write 어댑터.
 * '활성 계획'은 같은 (enrollment_id, week_no) 중 generated_at 이 가장 최신인 weekly_schedule 스냅샷으로 본다
 * (리플로우 이력이 누적되므로 과거 스냅샷은 제외).
 */
@Component
@RequiredArgsConstructor
public class SchedulePlanAdapter implements SchedulePlanPort {

    // 같은 enrollment+week_no 중 최신 weekly_schedule 스냅샷만 남기는 상관 서브쿼리(별칭 ws 기준).
    private static final String LATEST_SNAPSHOT_PREDICATE = """
            not exists (
                select 1 from weekly_schedule newer
                where newer.enrollment_id = ws.enrollment_id
                  and newer.week_no = ws.week_no
                  and (newer.generated_at > ws.generated_at
                       or (newer.generated_at = ws.generated_at and newer.id > ws.id))
            )
            """;

    private final EntityManager entityManager;

    @Override
    public List<ScheduleDtos.CalendarItem> findSlots(Long memberId, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select ss.id,
                       ss.plan_date,
                       ss.start_time,
                       ws.enrollment_id,
                       e.course_id,
                       c.subject,
                       c.title,
                       ss.lesson_id,
                       l.title,
                       ss.planned_min,
                       ss.status
                from schedule_slot ss
                join weekly_schedule ws on ws.id = ss.weekly_schedule_id
                join enrollment e on e.enrollment_id = ws.enrollment_id
                join course c on c.course_id = e.course_id
                join lesson l on l.id = ss.lesson_id
                where e.member_id = :memberId
                  and ss.plan_date between :from and :to
                  and %s
                order by ss.plan_date, ss.start_time, ss.id
                """.formatted(LATEST_SNAPSHOT_PREDICATE))
                .setParameter("memberId", memberId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<ScheduleDtos.CalendarItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            items.add(new ScheduleDtos.CalendarItem(
                    toLong(row[0]),
                    toLocalDate(row[1]),
                    toLocalTime(row[2]),
                    toLong(row[3]),
                    toLong(row[4]),
                    (String) row[5],
                    (String) row[6],
                    toLong(row[7]),
                    (String) row[8],
                    (int) toLong(row[9]),
                    (String) row[10]
            ));
        }
        return items;
    }

    @Override
    public int markSlotDone(Long memberId, Long slotId) {
        // 본인 소유(enrollment.member_id) 슬롯만 갱신. 소유 아니면 0행.
        return entityManager.createNativeQuery("""
                update schedule_slot ss
                join weekly_schedule ws on ws.id = ss.weekly_schedule_id
                join enrollment e on e.enrollment_id = ws.enrollment_id
                set ss.status = 'DONE'
                where ss.id = :slotId and e.member_id = :memberId
                """)
                .setParameter("slotId", slotId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    // ----- helpers -----

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return null;
    }

    private static LocalTime toLocalTime(Object value) {
        if (value instanceof java.sql.Time time) {
            return time.toLocalTime();
        }
        if (value instanceof LocalTime localTime) {
            return localTime;
        }
        return null;
    }
}
