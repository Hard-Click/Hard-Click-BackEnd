package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.query.GetStudyTimeGrassQuery;
import com.wanted.backend.domain.grass.application.usecase.GetStudyTimeGrassUseCase;
import com.wanted.backend.domain.grass.domain.model.StudyTimeGrassStat;
import com.wanted.backend.domain.grass.domain.policy.MonthlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.policy.StudyTimeGrassLevelPolicy;
import com.wanted.backend.domain.grass.domain.policy.YearlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.repository.StudyTimeGrassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStudyTimeGrassService implements GetStudyTimeGrassUseCase {

    private final StudyTimeGrassRepository studyTimeGrassRepository;
    private final StudyTimeGrassLevelPolicy studyTimeGrassLevelPolicy;
    private final YearlyGrassPeriodPolicy yearlyGrassPeriodPolicy;
    private final MonthlyGrassPeriodPolicy monthlyGrassPeriodPolicy;
    private final Clock clock;

    @Override
    public List<StudyTimeGrassView> handle(GetStudyTimeGrassQuery query) {
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
        List<StudyTimeGrassStat> stats = queryEndDate.isBefore(startDate)
                ? List.of()
                : studyTimeGrassRepository.findByMemberIdAndDateBetween(query.memberId(), startDate, queryEndDate);

        Map<LocalDate, Integer> studySecondsByDate = stats.stream()
                .collect(Collectors.toMap(
                        StudyTimeGrassStat::statDate,
                        StudyTimeGrassStat::studySeconds,
                        Integer::sum
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    int studySeconds = studySecondsByDate.getOrDefault(date, 0);
                    return new StudyTimeGrassView(
                            date,
                            studySeconds,
                            studyTimeGrassLevelPolicy.calculate(studySeconds),
                            date.isAfter(today)
                    );
                })
                .toList();
    }
}
