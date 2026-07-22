package com.wanted.backend.global.common;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangesTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final Period ONE_YEAR = Period.ofYears(1);
    private static final ErrorCode INVALID = ErrorCode.SCHEDULE_DATE_RANGE_INVALID;
    private static final ErrorCode TOO_LONG = ErrorCode.SCHEDULE_DATE_RANGE_TOO_LONG;

    @Test
    void allowsNormalRange() {
        assertThatCode(() -> DateRanges.requireValidRange(FROM, FROM.plusDays(30), ONE_YEAR, INVALID, TOO_LONG))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsSingleDay() {
        assertThatCode(() -> DateRanges.requireValidRange(FROM, FROM, ONE_YEAR, INVALID, TOO_LONG))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReversedRange() {
        assertThatThrownBy(() -> DateRanges.requireValidRange(FROM, FROM.minusDays(1), ONE_YEAR, INVALID, TOO_LONG))
                .isInstanceOf(BusinessException.class)
                .hasMessage(INVALID.getMessage());
    }

    /** 경계 포함: [from, from+1년-1일] 은 허용. */
    @Test
    void allowsExactlyMaxPeriod() {
        assertThatCode(() -> DateRanges.requireValidRange(
                FROM, FROM.plus(ONE_YEAR).minusDays(1), ONE_YEAR, INVALID, TOO_LONG))
                .doesNotThrowAnyException();
    }

    /** 최대 기간에서 하루만 넘겨도 거부. */
    @Test
    void rejectsRangeExceedingMaxByOneDay() {
        assertThatThrownBy(() -> DateRanges.requireValidRange(
                FROM, FROM.plus(ONE_YEAR), ONE_YEAR, INVALID, TOO_LONG))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TOO_LONG.getMessage());
    }
}
