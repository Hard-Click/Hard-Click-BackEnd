package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.ReviewPlanPort;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import com.wanted.backend.global.common.DateRanges;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleService implements ScheduleUseCase {

    private static final String STATUS_DONE = "DONE";

    // 캘린더 조회 최대 기간 = 1년. 무제한 범위(예: 10년) 요청 시 대량 조회로 인한 OOM·성능 저하를 막는다.
    // study_timer 일별 조회(ST018)와 동일한 상한을 쓴다.
    private static final Period MAX_QUERY_PERIOD = Period.ofYears(1);

    /**
     * 정렬 기준: 날짜 → 시작 시각 → 항목 ID.
     *
     * <p>시작 시각이 없는 항목(AI가 시각을 안 붙였거나 학생이 시간 없이 적은 할 일)은 <b>맨 뒤</b>로 보낸다.
     * 타임테이블에 배치되지 않고 목록에만 보이는 항목이라 시간 있는 것들 사이에 끼면 순서가 어색해진다.
     */
    private static final Comparator<ScheduleDtos.CalendarItem> DISPLAY_ORDER =
            Comparator.comparing(ScheduleDtos.CalendarItem::planDate)
                    .thenComparing(ScheduleDtos.CalendarItem::startTime,
                            Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
                    .thenComparing(ScheduleDtos.CalendarItem::itemId,
                            Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private final SchedulePlanPort schedulePlanPort;
    private final StudentTodoPort studentTodoPort;
    private final ReviewPlanPort reviewPlanPort;

    /**
     * AI 슬롯 + 학생 할 일 + 복습 항목을 합쳐 화면 순서로 정렬한다.
     *
     * <p>합치는 걸 서버가 하는 이유: 진행률(done/total)이 소스를 모두 세야 하고,
     * 프론트가 여러 번 호출해 직접 병합·정렬하면 정렬 규칙이 화면마다 갈린다.
     */
    private List<ScheduleDtos.CalendarItem> mergedItems(
            Long memberId, LocalDate from, LocalDate to, List<ScheduleDtos.CalendarItem> reviews) {
        return Stream.of(
                        schedulePlanPort.findSlots(memberId, from, to).stream(),
                        studentTodoPort.findTodos(memberId, from, to).stream(),
                        reviews.stream())
                .flatMap(s -> s)
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDtos.CalendarItem> getMySchedule(Long memberId, LocalDate from, LocalDate to) {
        DateRanges.requireValidRange(from, to, MAX_QUERY_PERIOD,
                ErrorCode.SCHEDULE_DATE_RANGE_INVALID, ErrorCode.SCHEDULE_DATE_RANGE_TOO_LONG);
        // 캘린더는 복습을 예정일(due) 그 날짜에 노출한다 - findDueReviews 가 (코스, due날짜) 단위로 이미 나눠 준다.
        return mergedItems(memberId, from, to, reviewPlanPort.findDueReviews(memberId, from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleDtos.TodayView getMyToday(Long memberId, LocalDate today) {
        // 오늘 목록의 복습은 '오늘까지 밀린 것 전부'(from=null)를 코스당 한 줄로 접어 올린다.
        List<ScheduleDtos.CalendarItem> reviews =
                collapseByCourse(reviewPlanPort.findDueReviews(memberId, null, today), today);
        List<ScheduleDtos.CalendarItem> items = mergedItems(memberId, today, today, reviews);
        // 진행률(done/total)은 완료 체크가 가능한 소스(LESSON/TODO)만 센다.
        // 복습은 체크박스로 완료하지 않고 유사퀴즈로 넘어가는 항목이라 분모에서 제외한다.
        List<ScheduleDtos.CalendarItem> countable = items.stream()
                .filter(item -> item.source() != ScheduleItemSource.REVIEW)
                .toList();
        int done = (int) countable.stream().filter(item -> STATUS_DONE.equals(item.status())).count();
        return new ScheduleDtos.TodayView(items, done, countable.size());
    }

    /**
     * 코스당 하나로 접는다(오늘 목록용). 같은 코스에 밀린 due 가 여러 날 있어도 한 줄만 남기고,
     * 노출 날짜는 오늘로 통일한다("오늘 복습할 것 있음" 어포던스).
     */
    private static List<ScheduleDtos.CalendarItem> collapseByCourse(
            List<ScheduleDtos.CalendarItem> reviews, LocalDate today) {
        Map<Long, ScheduleDtos.CalendarItem> byCourse = new LinkedHashMap<>();
        for (ScheduleDtos.CalendarItem review : reviews) {
            byCourse.computeIfAbsent(review.courseId(), courseId ->
                    ScheduleDtos.CalendarItem.ofReview(
                            review.courseId(), today, review.courseTitle(), review.status()));
        }
        return new ArrayList<>(byCourse.values());
    }

    @Override
    @Transactional
    public void completeSlot(Long memberId, Long slotId) {
        // TODO(reflow 연동): 완료 시 daily_achievement 기록까지 해야 야간/주간 리플로우가 실측을 반영한다.
        //  '오늘 목표 달성' 판정 규칙이 확정되면 이 지점에서 daily_achievement upsert 추가.
        int updated = schedulePlanPort.markSlotDone(memberId, slotId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCHEDULE_SLOT_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public int markPastPlannedAsMissed(LocalDate today) {
        // 계획일이 지난 PLANNED 슬롯을 MISSED 로. 배치(ScheduleMissedScheduler)가 매일 호출한다.
        return schedulePlanPort.markMissedBefore(today);
    }
}
