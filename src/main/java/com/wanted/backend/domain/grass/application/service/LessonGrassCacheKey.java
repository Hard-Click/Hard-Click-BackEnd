package com.wanted.backend.domain.grass.application.service;

import java.time.LocalDate;

/**
 * grassLessons 캐시 키 생성의 단일 소스.
 *
 * <p>조회(GetLessonGrassService.resolveCacheKey)와 무효화(LessonGrassStatUpdater.evict)가 이 규칙을 공유해야
 * 어긋나지 않는다. 과거 연도는 오늘이 바뀌어도 결과가 고정이라 연/월만으로 키를 고정하고, 현재(또는 미래) 연도만
 * 오늘 날짜를 suffix 로 붙여 day 단위로 갱신한다. month 는 제로패딩 없이 그대로 붙인다(1월 = "2026-1").
 */
final class LessonGrassCacheKey {

    private LessonGrassCacheKey() {
    }

    static String yearly(Long memberId, int year, LocalDate today) {
        return withTodaySuffix(memberId + ":" + year, year, today);
    }

    static String monthly(Long memberId, int year, int month, LocalDate today) {
        return withTodaySuffix(memberId + ":" + year + "-" + month, year, today);
    }

    private static String withTodaySuffix(String base, int year, LocalDate today) {
        return year >= today.getYear() ? base + ":" + today : base;
    }
}
