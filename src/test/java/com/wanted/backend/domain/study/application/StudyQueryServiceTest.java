package com.wanted.backend.domain.study.application;

import com.wanted.backend.domain.study.application.port.ChatRoomQueryPort;
import com.wanted.backend.domain.study.application.port.MemberNamePort;
import com.wanted.backend.domain.study.application.result.StudyDetailResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import com.wanted.backend.domain.study.application.service.StudyQueryService;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudyQueryServiceTest {

    @InjectMocks
    private StudyQueryService studyQueryService;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyParticipantRepository studyParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatRoomQueryPort chatRoomQueryPort;

    private Study activeStudy() {
        return Study.restore(45L, 1L, "수학 1등급 목표 스터디", "MATH_1",
                "매주 일요일 밤 10시에 모여서 질문 받습니다.", 5, 2, StudyStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("참여 중인 회원이 조회하면 참여자 목록과 chatRoomId가 함께 반환된다")
    void getDetail_success_joinedMember() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));
        given(studyParticipantRepository.findMemberIdsByStudyId(45L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(chatRoomQueryPort.findChatRoomIdByStudyId(45L)).willReturn(Optional.of(12L));

        // when
        StudyDetailResult result = studyQueryService.getDetail(45L, 2L);

        // then
        assertThat(result.isMine()).isFalse();
        assertThat(result.isJoined()).isTrue();
        assertThat(result.authorName()).isEqualTo("이*연");
        assertThat(result.members()).containsExactly("이*연", "김*수");
        assertThat(result.chatRoomId()).isEqualTo(12L);
        assertThat(result.isClosed()).isFalse();
    }

    @Test
    @DisplayName("방장이 조회하면 isMine이 true다")
    void getDetail_success_owner() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));
        given(studyParticipantRepository.findMemberIdsByStudyId(45L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(chatRoomQueryPort.findChatRoomIdByStudyId(45L)).willReturn(Optional.of(12L));

        // when
        StudyDetailResult result = studyQueryService.getDetail(45L, 1L);

        // then
        assertThat(result.isMine()).isTrue();
        assertThat(result.isJoined()).isTrue();
    }

    @Test
    @DisplayName("참여 중이 아닌 회원이 조회하면 members는 null이다")
    void getDetail_success_notJoined_membersHidden() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));
        given(studyParticipantRepository.findMemberIdsByStudyId(45L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(chatRoomQueryPort.findChatRoomIdByStudyId(45L)).willReturn(Optional.of(12L));

        // when
        StudyDetailResult result = studyQueryService.getDetail(45L, 999L);

        // then
        assertThat(result.isJoined()).isFalse();
        assertThat(result.members()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 스터디를 조회하면 예외가 발생한다")
    void getDetail_fail_notFound() {
        // given
        given(studyRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyQueryService.getDetail(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이름 조회 포트가 예외를 던지면 알 수 없음으로 대체된다")
    void getDetail_success_memberNamePortFails() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));
        given(studyParticipantRepository.findMemberIdsByStudyId(45L)).willReturn(List.of(1L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willThrow(new RuntimeException("down"));
        given(chatRoomQueryPort.findChatRoomIdByStudyId(45L)).willReturn(Optional.empty());

        // when
        StudyDetailResult result = studyQueryService.getDetail(45L, 1L);

        // then
        assertThat(result.authorName()).isEqualTo("알 수 없음");
        assertThat(result.chatRoomId()).isNull();
    }

    @Test
    @DisplayName("스터디 목록 조회 시 작성자 이름이 마스킹되어 반환된다")
    void getList_success_masksAuthorName() {
        // given
        Study study = Study.restore(101L, 1L, "주말 React 스터디 모집", "MATH_1", "강남 카페에서 진행합니다",
                6, 3, StudyStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        given(studyRepository.findAll(null, 0, 10)).willReturn(List.of(study));
        given(studyRepository.countAll(null)).willReturn(1);
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "최지훈"));

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
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of());

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
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willThrow(new RuntimeException("member service down"));

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
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "지훈"));

        // when
        StudyListResult result = studyQueryService.getList(null, 0, 10);

        // then
        assertThat(result.items().get(0).authorName()).isEqualTo("지*");
    }
}
