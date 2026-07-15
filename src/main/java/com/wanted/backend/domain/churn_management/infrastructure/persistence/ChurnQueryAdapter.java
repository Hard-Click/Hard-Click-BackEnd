package com.wanted.backend.domain.churn_management.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;
import com.wanted.backend.domain.churn_management.application.dto.ChurnReasonType;
import com.wanted.backend.domain.churn_management.application.port.ChurnQueryPort;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * dropout_risk(=Python-Server 야간 배치 산출물) 및 확정 스키마(enrollment/members/daily_achievement/
 * weekly_schedule/schedule_slot)를 native 로 read 하는 어댑터. 위험도 분류 임계값(0.7/0.4)은
 * Python domain/risk.py::risk_label 과 동일하게 맞춘다.
 *
 * <p>미확정/타 도메인 연동 필요분은 TODO 로 표시(퀴즈 평균 등) — 확정 시 이 파일만 수정한다.
 */
@Component
@RequiredArgsConstructor
public class ChurnQueryAdapter implements ChurnQueryPort {

    private static final double HIGH_THRESHOLD = 0.7;
    private static final double MEDIUM_THRESHOLD = 0.4;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 각 enrollment 의 '최신' dropout_risk 행만 남기는 상관 서브쿼리(동시각이면 id 큰 쪽). 별칭 r 고정.
    private static final String LATEST_PREDICATE = """
            not exists (
                select 1 from dropout_risk newer
                where newer.enrollment_id = r.enrollment_id
                  and (newer.computed_at > r.computed_at
                       or (newer.computed_at = r.computed_at and newer.id > r.id))
            )
            """;

    private final EntityManager entityManager;

    @Override
    public ChurnDashboardDtos.Summary findSummary() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                select
                    coalesce(sum(case when r.risk_score >= :high then 1 else 0 end), 0) as high_cnt,
                    coalesce(sum(case when r.risk_score >= :medium and r.risk_score < :high then 1 else 0 end), 0) as medium_cnt,
                    coalesce(avg(r.risk_score), 0) as avg_score
                from dropout_risk r
                where %s
                """.formatted(LATEST_PREDICATE))
                .setParameter("high", HIGH_THRESHOLD)
                .setParameter("medium", MEDIUM_THRESHOLD)
                .getSingleResult();

        long newThisWeek = toLong(entityManager.createNativeQuery("""
                select count(*) from (
                    select enrollment_id, min(computed_at) as first_at
                    from dropout_risk
                    group by enrollment_id
                    having first_at >= :weekStart
                ) t
                """)
                // '이번 주 신규' = 이번 주(월요일 00:00 이후)에 처음으로 위험 산출된 수강생 수.
                .setParameter("weekStart", startOfCurrentWeek())
                .getSingleResult());

        return new ChurnDashboardDtos.Summary(
                toLong(row[0]),
                toLong(row[1]),
                newThisWeek,
                toDouble(row[2])
        );
    }

    @Override
    public List<ChurnDashboardDtos.TrendPoint> findTrend(int weeks) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select date(date_sub(r.computed_at, interval weekday(r.computed_at) day)) as week_start,
                       count(distinct r.enrollment_id) as cnt
                from dropout_risk r
                where r.risk_score >= :high
                  and r.computed_at >= :since
                group by week_start
                order by week_start
                """)
                .setParameter("high", HIGH_THRESHOLD)
                .setParameter("since", startOfCurrentWeek().minusWeeks(weeks - 1L))
                .getResultList();

        List<ChurnDashboardDtos.TrendPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            points.add(new ChurnDashboardDtos.TrendPoint(toLocalDate(row[0]), toLong(row[1])));
        }
        return points;
    }

    @Override
    public List<ChurnDashboardDtos.ReasonRatio> findReasons() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select coalesce(json_unquote(json_extract(r.features, '$.top_reason')), 'etc') as reason,
                       count(*) as cnt
                from dropout_risk r
                where %s and r.risk_score >= :medium
                group by reason
                order by cnt desc
                """.formatted(LATEST_PREDICATE))
                .setParameter("medium", MEDIUM_THRESHOLD)
                .getResultList();

        long total = rows.stream().mapToLong(row -> toLong(row[1])).sum();

        List<ChurnDashboardDtos.ReasonRatio> reasons = new ArrayList<>();
        for (Object[] row : rows) {
            String code = (String) row[0];
            long count = toLong(row[1]);
            double ratio = total == 0 ? 0.0 : (double) count / total;
            reasons.add(new ChurnDashboardDtos.ReasonRatio(
                    code, ChurnReasonType.labelOf(code), count, ratio));
        }
        return reasons;
    }

    @Override
    public ChurnDashboardDtos.StudentPage findStudents(String level, int page, int size) {
        String levelFilter = levelFilter(level);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select r.enrollment_id,
                       e.member_id,
                       m.name,
                       r.risk_score,
                       r.computed_at,
                       json_unquote(json_extract(r.features, '$.top_reason')) as top_reason,
                       (select max(da.achieved_date) from daily_achievement da
                        where da.enrollment_id = r.enrollment_id) as last_activity
                from dropout_risk r
                join enrollment e on e.enrollment_id = r.enrollment_id
                join members m on m.member_id = e.member_id
                where %s and %s
                order by r.risk_score desc, r.enrollment_id
                limit :size offset :offset
                """.formatted(LATEST_PREDICATE, levelFilter))
                .setParameter("size", size)
                .setParameter("offset", (long) page * size)
                .getResultList();

        long total = toLong(entityManager.createNativeQuery("""
                select count(*)
                from dropout_risk r
                where %s and %s
                """.formatted(LATEST_PREDICATE, levelFilter))
                .getSingleResult());

        List<ChurnDashboardDtos.StudentRow> content = new ArrayList<>();
        for (Object[] row : rows) {
            double score = toDouble(row[3]);
            String reasonCode = (String) row[5];
            content.add(new ChurnDashboardDtos.StudentRow(
                    toLong(row[0]),
                    toLong(row[1]),
                    (String) row[2],
                    levelOf(score),
                    score,
                    reasonCode,
                    ChurnReasonType.labelOf(reasonCode),
                    toLocalDate(row[6]),
                    toLocalDateTime(row[4])
            ));
        }
        return new ChurnDashboardDtos.StudentPage(content, page, size, total);
    }

    @Override
    public ChurnDashboardDtos.StudentDetail findStudentDetail(Long enrollmentId) {
        List<?> result = entityManager.createNativeQuery("""
                select r.enrollment_id,
                       e.member_id,
                       m.name,
                       m.email,
                       r.risk_score,
                       r.computed_at,
                       r.features
                from dropout_risk r
                join enrollment e on e.enrollment_id = r.enrollment_id
                join members m on m.member_id = e.member_id
                where r.enrollment_id = :enrollmentId and %s
                """.formatted(LATEST_PREDICATE))
                .setParameter("enrollmentId", enrollmentId)
                .setMaxResults(1)
                .getResultList();

        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.CHURN_RISK_NOT_FOUND);
        }

        Object[] row = (Object[]) result.get(0);
        double score = toDouble(row[4]);

        return new ChurnDashboardDtos.StudentDetail(
                toLong(row[0]),
                toLong(row[1]),
                (String) row[2],
                (String) row[3],
                levelOf(score),
                score,
                toLocalDateTime(row[5]),
                parseContributions((String) row[6]),
                findProgressRate(enrollmentId),
                findLastAccessDate(enrollmentId),
                null, // TODO: 최근 퀴즈 평균 - 퀴즈 도메인 연동 후 채운다.
                findTotalStudyMinutes(enrollmentId)
        );
    }

    @Override
    public Long findMemberIdByEnrollmentId(Long enrollmentId) {
        List<?> result = entityManager.createNativeQuery("""
                select e.member_id from enrollment e where e.enrollment_id = :enrollmentId
                """)
                .setParameter("enrollmentId", enrollmentId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : toLong(result.get(0));
    }

    // ----- 상세 화면 '학습 현황' 보조 조회(확정 테이블 기반) -----

    private Double findProgressRate(Long enrollmentId) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                select coalesce(sum(case when ss.status = 'DONE' then 1 else 0 end), 0) as done_cnt,
                       count(*) as total_cnt
                from schedule_slot ss
                join weekly_schedule ws on ws.id = ss.weekly_schedule_id
                where ws.enrollment_id = :enrollmentId
                """)
                .setParameter("enrollmentId", enrollmentId)
                .getSingleResult();

        long done = toLong(row[0]);
        long total = toLong(row[1]);
        return total == 0 ? null : (double) done / total;
    }

    private LocalDate findLastAccessDate(Long enrollmentId) {
        Query query = entityManager.createNativeQuery("""
                select max(da.achieved_date)
                from daily_achievement da
                where da.enrollment_id = :enrollmentId
                """)
                .setParameter("enrollmentId", enrollmentId);
        return toLocalDate(query.getSingleResult());
    }

    private Integer findTotalStudyMinutes(Long enrollmentId) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(sum(da.actual_min), 0)
                from daily_achievement da
                where da.enrollment_id = :enrollmentId
                """)
                .setParameter("enrollmentId", enrollmentId);
        return toInteger(query.getSingleResult());
    }

    // ----- helpers -----

    private static String levelFilter(String level) {
        if (level == null) {
            return "r.risk_score >= " + MEDIUM_THRESHOLD;
        }
        return switch (level.toUpperCase()) {
            case "HIGH" -> "r.risk_score >= " + HIGH_THRESHOLD;
            case "MEDIUM" -> "r.risk_score >= " + MEDIUM_THRESHOLD + " and r.risk_score < " + HIGH_THRESHOLD;
            default -> "r.risk_score >= " + MEDIUM_THRESHOLD; // ALL(전체) = 위험군(>=중위험)
        };
    }

    private static String levelOf(double score) {
        if (score >= HIGH_THRESHOLD) {
            return "HIGH";
        }
        if (score >= MEDIUM_THRESHOLD) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static Map<String, Double> parseContributions(String featuresJson) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        if (featuresJson == null || featuresJson.isBlank()) {
            return contributions;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(featuresJson).path("contributions");
            node.fields().forEachRemaining(entry ->
                    contributions.put(entry.getKey(), entry.getValue().asDouble()));
        } catch (Exception ignored) {
            // features 스키마가 예상과 다르면 기여도 없이 총점만 표시(방어적).
        }
        return contributions;
    }

    private static LocalDateTime startOfCurrentWeek() {
        // 이번 주 월요일 00:00. computed_at(DATETIME) 비교용.
        return LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay();
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return null;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }
}
