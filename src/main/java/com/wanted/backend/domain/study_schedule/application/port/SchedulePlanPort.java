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

    /**
     * 계획일이 지났는데(plan_date &lt; today) 아직 PLANNED 인 슬롯을 MISSED 로 일괄 전이한다.
     * 매일 배치가 호출 - "하루 지나면 못 한 학습으로 표시"의 원천. DONE/이미 MISSED 는 건드리지 않는다.
     * @return 갱신된 행 수
     */
    int markMissedBefore(LocalDate today);
}
