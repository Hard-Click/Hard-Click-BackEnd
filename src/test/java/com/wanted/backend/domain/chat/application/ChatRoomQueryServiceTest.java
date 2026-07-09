package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.port.ChatPresencePort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.port.StudyInfoQueryPort;
import com.wanted.backend.domain.chat.application.port.StudyInfoResult;
import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.service.ChatRoomQueryService;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatRoomQueryServiceTest {

    @InjectMocks
    private ChatRoomQueryService chatRoomQueryService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatPresencePort chatPresencePort;

    @Mock
    private StudyInfoQueryPort studyInfoQueryPort;

    private ChatRoom activeRoom() {
        return ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("참여자가 조회하면 스터디 정보와 참여자별 접속 여부가 함께 반환된다")
    void getRoom_success() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 1등급 목표 스터디", "MATH_1")));
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(12L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(chatPresencePort.getOnlineMemberIds(12L)).willReturn(Set.of(1L));

        // when
        ChatRoomDetailResult result = chatRoomQueryService.getRoom(12L, 1L);

        // then
        assertThat(result.chatRoomId()).isEqualTo(12L);
        assertThat(result.groupId()).isEqualTo(45L);
        assertThat(result.title()).isEqualTo("수학 1등급 목표 스터디");
        assertThat(result.subjectName()).isEqualTo("MATH_1");
        assertThat(result.hostId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.participants()).extracting("memberId", "name", "online")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "이*연", true),
                        org.assertj.core.groups.Tuple.tuple(2L, "김*수", false));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 조회하면 예외가 발생한다")
    void getRoom_fail_notFound() {
        // given
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomQueryService.getRoom(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 예외가 발생한다")
    void getRoom_fail_notParticipant() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatRoomQueryService.getRoom(12L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("연결된 스터디를 찾을 수 없으면 예외가 발생한다")
    void getRoom_fail_studyNotFound() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomQueryService.getRoom(12L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이름 조회 포트가 예외를 던지면 알 수 없음으로 대체된다")
    void getRoom_success_memberNamePortFails() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 1등급 목표 스터디", "MATH_1")));
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(12L)).willReturn(List.of(1L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willThrow(new RuntimeException("down"));
        given(chatPresencePort.getOnlineMemberIds(12L)).willReturn(Set.of());

        // when
        ChatRoomDetailResult result = chatRoomQueryService.getRoom(12L, 1L);

        // then
        assertThat(result.participants()).hasSize(1);
        assertThat(result.participants().get(0).name()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("presence 포트가 예외를 던지면 모든 참여자가 오프라인으로 처리된다")
    void getRoom_success_presencePortFails() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 1등급 목표 스터디", "MATH_1")));
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(12L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(chatPresencePort.getOnlineMemberIds(12L)).willThrow(new RuntimeException("presence down"));

        // when
        ChatRoomDetailResult result = chatRoomQueryService.getRoom(12L, 1L);

        // then
        assertThat(result.participants()).extracting("online")
                .containsExactly(false, false);
    }
}
