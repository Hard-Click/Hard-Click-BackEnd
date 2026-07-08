package com.wanted.backend.domain.study.application;

import com.wanted.backend.domain.study.application.port.MemberNamePort;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import com.wanted.backend.domain.study.application.service.StudyQueryService;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anySet;

@ExtendWith(MockitoExtension.class)
class StudyQueryServiceTest {

    @InjectMocks
    private StudyQueryService studyQueryService;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Test
    @DisplayName("스터디 목록 조회 시 작성자 이름이 마스킹되어 반환된다")
    void getList_success_masksAuthorName() {
        // given
        Study study = Study.restore(101L, 1L, "주말 React 스터디 모집", "MATH_1", "강남 카페에서 진행합니다",
                6, 3, StudyStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        given(studyRepository.findAll(null, 0, 10)).willReturn(List.of(study));
        given(studyRepository.countAll(null)).willReturn(1);
        given(memberNamePort.getNamesByMemberIds(anySet())).willReturn(Map.of(1L, "최지훈"));

        // when
        StudyListResult result = studyQueryService.getList(null, 0, 10);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).authorName()).isEqualTo("최*훈");
        assertThat(result.items().get(0).isClosed()).isFalse();
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("결과가 없으면 빈 목록과 0페이지를 반환한다")
    void getList_success_empty() {
        // given
        given(studyRepository.findAll("MATH_1", 0, 10)).willReturn(List.of());
        given(studyRepository.countAll("MATH_1")).willReturn(0);

        // when
        StudyListResult result = studyQueryService.getList("MATH_1", 0, 10);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("작성자 이름을 찾을 수 없으면 알 수 없음을 반환한다")
    void getList_success_hostNameNotFound() {
        // given
        Study study = Study.restore(101L, 1L, "주말 React 스터디 모집", "MATH_1", "강남 카페에서 진행합니다",
                6, 3, StudyStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        given(studyRepository.findAll(null, 0, 10)).willReturn(List.of(study));
        given(studyRepository.countAll(null)).willReturn(1);
        given(memberNamePort.getNamesByMemberIds(anySet())).willReturn(Map.of());

        // when
        StudyListResult result = studyQueryService.getList(null, 0, 10);

        // then
        assertThat(result.items().get(0).authorName()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("이름 조회 포트가 예외를 던지면 알 수 없음으로 대체되고 조회는 실패하지 않는다")
    void getList_success_memberNamePortFails() {
        // given
        Study study = Study.restore(101L, 1L, "주말 React 스터디 모집", "MATH_1", "강남 카페에서 진행합니다",
                6, 3, StudyStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        given(studyRepository.findAll(null, 0, 10)).willReturn(List.of(study));
        given(studyRepository.countAll(null)).willReturn(1);
        given(memberNamePort.getNamesByMemberIds(anySet())).willThrow(new RuntimeException("member service down"));

        // when
        StudyListResult result = studyQueryService.getList(null, 0, 10);

        // then
        assertThat(result.items().get(0).authorName()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("totalPages가 올바르게 계산된다")
    void getList_success_multiplePages() {
        // given
        given(studyRepository.findAll("MATH_1", 0, 10)).willReturn(List.of());
        given(studyRepository.countAll("MATH_1")).willReturn(25);

        // when
        StudyListResult result = studyQueryService.getList("MATH_1", 0, 10);

        // then
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("이름이 2글자면 마지막 글자만 마스킹한다")
    void getList_success_masksTwoCharName() {
        // given
        Study study = Study.restore(101L, 1L, "주말 React 스터디 모집", "MATH_1", "강남 카페에서 진행합니다",
                6, 3, StudyStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        given(studyRepository.findAll(null, 0, 10)).willReturn(List.of(study));
        given(studyRepository.countAll(null)).willReturn(1);
        given(memberNamePort.getNamesByMemberIds(anySet())).willReturn(Map.of(1L, "지훈"));

        // when
        StudyListResult result = studyQueryService.getList(null, 0, 10);

        // then
        assertThat(result.items().get(0).authorName()).isEqualTo("지*");
    }
}
