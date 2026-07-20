package com.wanted.backend.domain.study_schedule.application.port;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * FSRS 복습 카드(review_card) 조회 아웃바운드 포트. 복습 예정일(due)에 도달한 카드를 코스 단위로 묶어
 * '복습' 스케줄 항목으로 노출한다. 완료 갱신은 유사퀴즈 제출 쪽 책임이라 이 포트는 읽기 전용이다.
 */
public interface ReviewPlanPort {

    /**
     * 회원의 복습 카드 중 due 가 [from, to] 안에 든 것을 코스별로 묶어 반환한다.
     *
     * @param from 하한(포함). null 이면 하한 없음 - 지난 예정일까지 모두 포함(밀린 복습을 '오늘'로 끌어올릴 때 사용).
     * @param to   상한(포함).
     */
    List<ScheduleDtos.CalendarItem> findDueReviews(Long memberId, LocalDate from, LocalDate to);
}
