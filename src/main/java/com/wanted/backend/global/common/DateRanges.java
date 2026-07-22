package com.wanted.backend.global.common;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;

import java.time.LocalDate;
import java.time.Period;

/**
 * 기간 조회(from~to) 파라미터 공통 검증.
 *
 * <p>스케줄·study_timer 등 여러 도메인이 "역전 범위 거부 + 최대 조회 기간 상한"을 동일하게 필요로 해
 * 한곳으로 모은다. 도메인별 상한(max)과 던질 ErrorCode 는 인자로 주입받아, 에러코드·메시지는
 * 각 도메인 규칙(SC005/006, ST017/018 등)을 그대로 유지한다.
 */
public final class DateRanges {

    private DateRanges() {
    }

    /**
     * [from, to] 조회 범위를 검증한다. 경계 포함 최대 허용 범위는 {@code [from, from+max-1일]}.
     * (from/to 는 non-null 가정 — null 여부는 호출 측에서 도메인 규칙대로 먼저 검증한다.)
     *
     * @param max     허용 최대 기간(경계 포함)
     * @param invalid {@code from > to} 역전일 때 던질 에러코드
     * @param tooLong 최대 기간을 초과할 때 던질 에러코드
     */
    public static void requireValidRange(
            LocalDate from, LocalDate to, Period max, ErrorCode invalid, ErrorCode tooLong) {
        if (from.isAfter(to)) {
            throw new BusinessException(invalid);
        }
        if (to.isAfter(from.plus(max).minusDays(1))) {
            throw new BusinessException(tooLong);
        }
    }
}
