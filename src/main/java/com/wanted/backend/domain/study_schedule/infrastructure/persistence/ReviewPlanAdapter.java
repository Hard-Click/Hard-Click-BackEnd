package com.wanted.backend.domain.study_schedule.infrastructure.persistence;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.ReviewPlanPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * review_card(+enrollment/course) native read 어댑터.
 *
 * <p>복습 카드는 (enrollment, lesson) 단위지만 화면·유사퀴즈는 코스 단위로 동작하므로, 같은 코스의 카드를
 * (코스, due 날짜)로 묶어 하루당 한 줄로 노출한다. 완료 상태는 스케줄이 소유하지 않아 항상 PLANNED 로 둔다
 * (실제 복습 완료는 유사퀴즈 제출로 review_card 가 갱신되며, 그러면 due 가 미래로 밀려 목록에서 빠진다).
 */
@Component
@RequiredArgsConstructor
public class ReviewPlanAdapter implements ReviewPlanPort {

    private final EntityManager entityManager;

    @Override
    public List<ScheduleDtos.CalendarItem> findDueReviews(Long memberId, LocalDate from, LocalDate to) {
        // from 이 null 이면 하한 없음(밀린 복습까지 포함). date(due) 로 비교해 시각과 무관하게 '그 날 예정'을 잡는다.
        String fromPredicate = (from == null) ? "" : " and date(rc.due) >= :from ";
        Query query = entityManager.createNativeQuery("""
                select e.course_id,
                       c.title,
                       date(rc.due) as due_date
                from review_card rc
                join enrollment e on e.enrollment_id = rc.enrollment_id
                join course c on c.course_id = e.course_id
                where e.member_id = :memberId
                  and rc.due is not null
                  and date(rc.due) <= :to
                  %s
                group by e.course_id, c.title, date(rc.due)
                order by due_date, e.course_id
                """.formatted(fromPredicate))
                .setParameter("memberId", memberId)
                .setParameter("to", to);
        if (from != null) {
            query.setParameter("from", from);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<ScheduleDtos.CalendarItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            items.add(ScheduleDtos.CalendarItem.ofReview(
                    toLong(row[0]),
                    toLocalDate(row[2]),
                    (String) row[1],
                    "PLANNED"));
        }
        return items;
    }

    // ----- helpers -----

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
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
}
