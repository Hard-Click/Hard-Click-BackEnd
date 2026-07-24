package com.wanted.backend.domain.quiz.infrastructure.review;

import com.wanted.backend.domain.quiz.application.port.ReviewCompletionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * review_card 완료 전진 어댑터(native write). quiz 도메인이 study_schedule 의 복습 카드를 직접 갱신하는
 * 결합을 인프라로 격리한다(ReviewPlanAdapter 의 읽기와 대칭되는 쓰기).
 *
 * <p>1차 스코프는 <b>고정 간격</b> 전진이다(reps 기반 3→7→14→30일). stability/difficulty 를 쓰는 정식 FSRS
 * 재학습은 후속 확장으로 미룬다 — 지금은 due 를 미래로 밀어 '오늘 복습 완료'만 성립시키면 된다.
 */
@Component
@RequiredArgsConstructor
public class ReviewCompletionAdapter implements ReviewCompletionPort {

    // reps(전진 전) 별 다음 복습까지의 간격(일). 마지막 값을 상한으로 clamp.
    private static final int[] INTERVAL_DAYS_BY_REPS = {3, 7, 14, 30};

    private final EntityManager entityManager;

    @Override
    @Transactional
    public int completeTodayReviews(Long memberId, Long courseId, LocalDateTime completedAt) {
        LocalDate today = completedAt.toLocalDate();
        // 내일 자정(exclusive) 상한. date(rc.due) 함수 대신 범위 비교를 써서 idx_card_due 인덱스를 살린다.
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        // 오늘까지 due 인(= 밀린 것 포함) 카드만 대상. 이미 완료돼 due 가 미래인 카드는 잡히지 않아 멱등하다.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select rc.id, rc.reps
                from review_card rc
                join enrollment e on e.enrollment_id = rc.enrollment_id
                where e.member_id = :memberId
                  and e.course_id = :courseId
                  and rc.due is not null
                  and rc.due < :tomorrowStart
                """)
                .setParameter("memberId", memberId)
                .setParameter("courseId", courseId)
                .setParameter("tomorrowStart", tomorrowStart)
                .getResultList();

        int advanced = 0;
        for (Object[] row : rows) {
            long cardId = ((Number) row[0]).longValue();
            int reps = ((Number) row[1]).intValue();
            int intervalDays = nextIntervalDays(reps);
            LocalDate nextDue = today.plusDays(intervalDays);

            // UPDATE 에도 due 상한을 걸어, 동시 제출로 다른 트랜잭션이 이미 전진시킨 카드는 0 rows 가 되게 한다
            // (SELECT 조건만으로는 두 트랜잭션이 같은 행을 읽어 reps 가 이중 증가할 수 있다).
            Query update = entityManager.createNativeQuery("""
                    update review_card
                    set reps = reps + 1,
                        last_review = :now,
                        due = :nextDue,
                        scheduled_days = :intervalDays,
                        state = 'REVIEW'
                    where id = :cardId
                      and due < :tomorrowStart
                    """)
                    .setParameter("now", completedAt)
                    .setParameter("nextDue", nextDue.atStartOfDay())
                    .setParameter("intervalDays", intervalDays)
                    .setParameter("cardId", cardId)
                    .setParameter("tomorrowStart", tomorrowStart);
            advanced += update.executeUpdate();
        }
        return advanced;
    }

    /**
     * 고정 간격 정책: reps(전진 전) 가 커질수록 간격을 늘린다. 배열 상한을 넘으면 마지막 값으로 clamp.
     * 도메인 규칙이라 단위 테스트가 직접 검증하도록 package-private static 으로 둔다.
     */
    static int nextIntervalDays(int reps) {
        int index = Math.min(Math.max(reps, 0), INTERVAL_DAYS_BY_REPS.length - 1);
        return INTERVAL_DAYS_BY_REPS[index];
    }
}
