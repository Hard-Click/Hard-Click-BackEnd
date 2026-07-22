package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.query.GetLessonGrassQuery;
import com.wanted.backend.domain.grass.application.usecase.GetLessonGrassUseCase;
import com.wanted.backend.domain.grass.domain.model.LessonGrassStat;
import com.wanted.backend.domain.grass.domain.policy.LessonGrassLevelPolicy;
import com.wanted.backend.domain.grass.domain.policy.MonthlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.policy.YearlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.repository.LessonGrassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetLessonGrassService implements GetLessonGrassUseCase {

    private final LessonGrassRepository lessonGrassRepository;
    private final LessonGrassLevelPolicy lessonGrassLevelPolicy;
    private final YearlyGrassPeriodPolicy yearlyGrassPeriodPolicy;
    private final MonthlyGrassPeriodPolicy monthlyGrassPeriodPolicy;
    private final Clock clock;

    @Override
    @Cacheable(cacheNames = "grassLessons:v3", key = "#root.target.resolveCacheKey(#query)")
    public List<LessonGrassView> handle(GetLessonGrassQuery query) {
        LocalDate today = LocalDate.now(clock);
        int year = query.year() != null ? query.year() : today.getYear();

        // month가 있으면 그 달만, 없으면 기존처럼 연간 전체 범위로 조회한다(응답 형식·지표는 동일).
        LocalDate startDate;
        LocalDate endDate;
        LocalDate queryEndDate;
        if (query.month() != null) {
            var period = monthlyGrassPeriodPolicy.calculate(year, query.month(), today);
            startDate = period.startDate();
            endDate = period.endDate();
            queryEndDate = period.queryEndDate();
        } else {
            var period = yearlyGrassPeriodPolicy.calculate(year, today);
            startDate = period.startDate();
            endDate = period.endDate();
            queryEndDate = period.queryEndDate();
        }

        // queryEndDate가 startDate 이전이면(전 구간 미래) 조회 없이 0으로 채운다.
        List<LessonGrassStat> stats = queryEndDate.isBefore(startDate)
                ? List.of()
                : lessonGrassRepository.findByMemberIdAndDateBetween(query.memberId(), startDate, queryEndDate);

        Map<LocalDate, Integer> watchedLessonCountByDate = stats.stream()
                .collect(Collectors.toMap(
                        LessonGrassStat::statDate,
                        LessonGrassStat::watchedLessonCount,
                        Integer::sum
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    int watchedLessonCount = watchedLessonCountByDate.getOrDefault(date, 0);
                    return new LessonGrassView(
                            date,
                            watchedLessonCount,
                            lessonGrassLevelPolicy.calculate(watchedLessonCount),
                            date.isAfter(today)
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 과거 연도는 오늘 날짜가 바뀌어도 결과가 달라지지 않으므로 연도(+월)만으로 키를 고정하고,
     * 현재(또는 미래) 연도만 오늘 날짜를 키에 포함해 day 단위로 갱신한다.
     * month 유무로 연간/월간 캐시 네임스페이스를 분리해 서로 덮어쓰지 않게 한다.
     */
    public String resolveCacheKey(GetLessonGrassQuery query) {
        LocalDate today = LocalDate.now(clock);
        int year = query.year() != null ? query.year() : today.getYear();
        String scope = query.month() != null ? year + "-" + query.month() : String.valueOf(year);
        String key = query.memberId() + ":" + scope;
        return year >= today.getYear() ? key + ":" + today : key;
    }
}
