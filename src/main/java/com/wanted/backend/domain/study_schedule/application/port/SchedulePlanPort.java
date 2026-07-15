package com.wanted.backend.domain.study_schedule.application.port;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * 학습 스케줄 조회/완료체크 아웃바운드 포트. 활성 계획(=같은 enrollment+week_no 중 최신 weekly_schedule 스냅샷)의
 * schedule_slot 을 읽고, 완료 체크는 슬롯 status 를 갱신한다.
 */
public interface SchedulePlanPort {

    /** 기간 내 회원의 활성 스케줄 슬롯 목록. */
    List<ScheduleDtos.CalendarItem> findSlots(Long memberId, LocalDate from, LocalDate to);

    /**
     * 슬롯을 완료(DONE) 처리. 본인 소유 슬롯만 갱신된다.
     * @return 갱신된 행 수(0이면 없음/타인 소유)
     */
    int markSlotDone(Long memberId, Long slotId);
}
