package com.wanted.backend.domain.study_timer.application.service;

import com.wanted.backend.domain.study_timer.application.query.GetDailyStudyTimeQuery;
import com.wanted.backend.domain.study_timer.application.usecase.GetDailyStudyTimeUseCase;
import com.wanted.backend.domain.study_timer.domain.model.DailyStudyStat;
import com.wanted.backend.domain.study_timer.domain.repository.DailyStudyStatsRepository;
import com.wanted.backend.global.common.DateRanges;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDailyStudyTimeService implements GetDailyStudyTimeUseCase {

    private static final Period MAX_DAILY_STUDY_TIME_QUERY_PERIOD = Period.ofYears(1);

    private final DailyStudyStatsRepository dailyStudyStatsRepository;

    @Override
    public List<DailyStudyTimeItem> handle(GetDailyStudyTimeQuery query) {
        validate(query);

        List<DailyStudyStat> stats = dailyStudyStatsRepository.findByMemberIdAndDateBetween(
                query.memberId(),
                query.startDate(),
                query.endDate()
        );

        Map<LocalDate, Integer> studySecondsByDate = stats.stream()
                .collect(Collectors.toMap(
                        DailyStudyStat::studyDate,
                        DailyStudyStat::studySeconds,
                        Integer::sum
                ));

        return query.startDate()
                .datesUntil(query.endDate().plusDays(1))
                .map(date -> new DailyStudyTimeItem(
                        date,
                        studySecondsByDate.getOrDefault(date, 0)
                ))
                .toList();
    }

    private void validate(GetDailyStudyTimeQuery query) {
        if (query.memberId() == null) {
            throw new BusinessException(ErrorCode.STUDY_TIMER_MEMBER_ID_REQUIRED);
        }
        if (query.startDate() == null) {
            throw new BusinessException(ErrorCode.STUDY_TIMER_DAILY_START_DATE_REQUIRED);
        }
        if (query.endDate() == null) {
            throw new BusinessException(ErrorCode.STUDY_TIMER_DAILY_END_DATE_REQUIRED);
        }
        DateRanges.requireValidRange(
                query.startDate(), query.endDate(), MAX_DAILY_STUDY_TIME_QUERY_PERIOD,
                ErrorCode.STUDY_TIMER_DAILY_DATE_RANGE_INVALID,
                ErrorCode.STUDY_TIMER_DAILY_DATE_RANGE_TOO_LONG);
    }
}
