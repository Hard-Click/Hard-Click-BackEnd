package com.wanted.backend.domain.grass.application.port;

import java.time.LocalDate;

/**
 * 레슨 완료 시 해당 날짜의 수강량 집계(daily_study_stats.watched_lesson_count)를 1 증가시킨다.
 *
 * <p>grass 는 daily_study_stats 를 이미 읽고 있고(LessonGrassRepository), 이 포트로 그 테이블의
 * 레슨 카운트만 원자적으로 증가시킨다. (study_seconds 는 study_timer 가 별도로 집계)
 */
public interface LessonGrassCountWriter {

    void increment(Long memberId, LocalDate statDate);
}
