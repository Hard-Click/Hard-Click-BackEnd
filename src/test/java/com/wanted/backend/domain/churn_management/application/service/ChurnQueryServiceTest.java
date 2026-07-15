package com.wanted.backend.domain.churn_management.application.service;

import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;
import com.wanted.backend.domain.churn_management.application.port.ChurnQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChurnQueryServiceTest {

    @InjectMocks
    private ChurnQueryService churnQueryService;

    @Mock
    private ChurnQueryPort churnQueryPort;

    @Test
    @DisplayName("요약 조회는 포트 결과를 그대로 반환한다")
    void getSummary_delegates() {
        // given
        ChurnDashboardDtos.Summary summary = new ChurnDashboardDtos.Summary(3, 5, 2, 0.42);
        given(churnQueryPort.findSummary()).willReturn(summary);

        // when
        ChurnDashboardDtos.Summary result = churnQueryService.getSummary();

        // then
        assertThat(result).isSameAs(summary);
        verify(churnQueryPort).findSummary();
    }

    @Test
    @DisplayName("추이 조회는 양수 주수를 그대로 포트에 전달한다")
    void getTrend_positiveWeeksPassThrough() {
        // given
        given(churnQueryPort.findTrend(4)).willReturn(List.of());

        // when
        churnQueryService.getTrend(4);

        // then
        verify(churnQueryPort).findTrend(4);
    }

    @Test
    @DisplayName("추이 조회 시 주수가 0 이하이면 기본값 8주로 보정한다")
    void getTrend_nonPositiveWeeksFallsBackToDefault() {
        // given
        given(churnQueryPort.findTrend(8)).willReturn(List.of());

        // when
        churnQueryService.getTrend(0);
        churnQueryService.getTrend(-5);

        // then
        verify(churnQueryPort, org.mockito.Mockito.times(2)).findTrend(8);
    }

    @Test
    @DisplayName("사유 비율 조회는 포트에 위임한다")
    void getReasons_delegates() {
        // given
        given(churnQueryPort.findReasons()).willReturn(List.of());

        // when
        churnQueryService.getReasons();

        // then
        verify(churnQueryPort).findReasons();
    }

    @Test
    @DisplayName("학생 목록 조회는 정상 파라미터를 그대로 전달한다")
    void getStudents_validParamsPassThrough() {
        // given
        ChurnDashboardDtos.StudentPage page = new ChurnDashboardDtos.StudentPage(List.of(), 1, 20, 0);
        given(churnQueryPort.findStudents("HIGH", 1, 20)).willReturn(page);

        // when
        ChurnDashboardDtos.StudentPage result = churnQueryService.getStudents("HIGH", 1, 20);

        // then
        assertThat(result).isSameAs(page);
        verify(churnQueryPort).findStudents("HIGH", 1, 20);
    }

    @Test
    @DisplayName("학생 목록 조회 시 음수 페이지는 0으로 보정한다")
    void getStudents_negativePageClampedToZero() {
        // given
        given(churnQueryPort.findStudents("HIGH", 0, 20)).willReturn(
                new ChurnDashboardDtos.StudentPage(List.of(), 0, 20, 0));

        // when
        churnQueryService.getStudents("HIGH", -3, 20);

        // then
        verify(churnQueryPort).findStudents("HIGH", 0, 20);
    }

    @Test
    @DisplayName("학생 목록 조회 시 size가 0 이하이면 기본 20으로 보정한다")
    void getStudents_nonPositiveSizeFallsBackTo20() {
        // given
        given(churnQueryPort.findStudents("HIGH", 0, 20)).willReturn(
                new ChurnDashboardDtos.StudentPage(List.of(), 0, 20, 0));

        // when
        churnQueryService.getStudents("HIGH", 0, 0);

        // then
        verify(churnQueryPort).findStudents("HIGH", 0, 20);
    }

    @Test
    @DisplayName("학생 목록 조회 시 size가 최대치(100)를 넘으면 100으로 제한한다")
    void getStudents_oversizeCappedTo100() {
        // given
        given(churnQueryPort.findStudents("HIGH", 0, 100)).willReturn(
                new ChurnDashboardDtos.StudentPage(List.of(), 0, 100, 0));

        // when
        churnQueryService.getStudents("HIGH", 0, 500);

        // then
        verify(churnQueryPort).findStudents("HIGH", 0, 100);
    }

    @Test
    @DisplayName("학생 상세 조회는 포트에 위임한다")
    void getStudentDetail_delegates() {
        // given
        given(churnQueryPort.findStudentDetail(77L)).willReturn(null);

        // when
        churnQueryService.getStudentDetail(77L);

        // then
        verify(churnQueryPort).findStudentDetail(77L);
    }
}
