package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.port.ChatPresencePort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.port.StudyInfoQueryPort;
import com.wanted.backend.domain.chat.application.port.StudyInfoResult;
import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.MyChatRoomDetail;
import com.wanted.backend.domain.chat.application.service.ChatRoomQueryService;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatMessageType;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
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
    private ChatMessageRepository chatMessageRepository;

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

    @Test
    @DisplayName("참여 중인 채팅방이 없으면 빈 목록을 반환한다")
    void getMyRooms_success_empty() {
        // given
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of());

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("마지막 메시지 전송 시각 최신순으로 정렬되어 반환된다")
    void getMyRooms_success_sortedByLastMessageAt() {
        // given
        ChatRoom room1 = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        ChatRoom room2 = ChatRoom.restore(13L, 46L, 2L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L, 13L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L, 13L))).willReturn(List.of(room1, room2));

        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 스터디", "MATH_1")));
        given(studyInfoQueryPort.getStudyInfo(46L)).willReturn(Optional.of(new StudyInfoResult("영어 스터디", "ENG_1")));

        ChatMessage olderMessage = ChatMessage.restore(1L, 12L, 1L, ChatMessageType.CHAT,
                "오래된 메시지", LocalDateTime.of(2026, 5, 10, 12, 0));
        ChatMessage newerMessage = ChatMessage.restore(2L, 13L, 2L, ChatMessageType.CHAT,
                "최근 메시지", LocalDateTime.of(2026, 5, 11, 12, 5));
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.of(olderMessage));
        given(chatMessageRepository.findLatestByChatRoomId(13L)).willReturn(Optional.of(newerMessage));
        given(chatMessageRepository.countUnreadByChatRoomIdAndMemberId(12L, 1L)).willReturn(0L);
        given(chatMessageRepository.countUnreadByChatRoomIdAndMemberId(13L, 1L)).willReturn(0L);

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).chatRoomId()).isEqualTo(13L);
        assertThat(result.get(0).name()).isEqualTo("영어 스터디");
        assertThat(result.get(0).lastMessage()).isEqualTo("최근 메시지");
        assertThat(result.get(0).unreadCount()).isEqualTo(0);
        assertThat(result.get(1).chatRoomId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("안 읽은 메시지가 있으면 unreadCount에 실제 개수가 반영된다")
    void getMyRooms_success_unreadCountReflectsRealValue() {
        // given
        ChatRoom room = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L))).willReturn(List.of(room));
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 스터디", "MATH_1")));
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.empty());
        given(chatMessageRepository.countUnreadByChatRoomIdAndMemberId(12L, 1L)).willReturn(3L);

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result.get(0).unreadCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("unreadCount 조회가 실패해도 0으로 대체되고 목록은 정상 반환된다")
    void getMyRooms_success_unreadCountPortThrows() {
        // given
        ChatRoom room = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L))).willReturn(List.of(room));
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 스터디", "MATH_1")));
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.empty());
        given(chatMessageRepository.countUnreadByChatRoomIdAndMemberId(12L, 1L)).willThrow(new RuntimeException("db down"));

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result.get(0).unreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("메시지가 없는 채팅방은 목록 뒤로 밀린다")
    void getMyRooms_success_noMessageRoomsLast() {
        // given
        ChatRoom roomWithMessage = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        ChatRoom roomWithoutMessage = ChatRoom.restore(13L, 46L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L, 13L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L, 13L))).willReturn(List.of(roomWithoutMessage, roomWithMessage));

        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 스터디", "MATH_1")));
        given(studyInfoQueryPort.getStudyInfo(46L)).willReturn(Optional.of(new StudyInfoResult("영어 스터디", "ENG_1")));

        ChatMessage message = ChatMessage.restore(1L, 12L, 1L, ChatMessageType.CHAT,
                "메시지", LocalDateTime.now());
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.of(message));
        given(chatMessageRepository.findLatestByChatRoomId(13L)).willReturn(Optional.empty());

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).chatRoomId()).isEqualTo(12L);
        assertThat(result.get(1).chatRoomId()).isEqualTo(13L);
        assertThat(result.get(1).lastMessage()).isNull();
        assertThat(result.get(1).lastMessageAt()).isNull();
    }

    @Test
    @DisplayName("연결된 스터디 정보를 찾을 수 없으면 이름을 알 수 없음으로 대체한다")
    void getMyRooms_success_studyInfoMissing() {
        // given
        ChatRoom room = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L))).willReturn(List.of(room));
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.empty());
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.empty());

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("studyInfoQueryPort가 예외를 던져도 해당 방만 알 수 없음으로 대체되고 나머지 목록은 정상 반환된다")
    void getMyRooms_success_studyInfoPortThrows() {
        // given
        ChatRoom room = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L))).willReturn(List.of(room));
        given(studyInfoQueryPort.getStudyInfo(45L)).willThrow(new RuntimeException("db down"));
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willReturn(Optional.empty());

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("chatMessageRepository가 예외를 던져도 마지막 메시지 없음으로 대체되고 목록은 정상 반환된다")
    void getMyRooms_success_chatMessageRepositoryThrows() {
        // given
        ChatRoom room = ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomParticipantRepository.findChatRoomIdsByMemberId(1L)).willReturn(List.of(12L));
        given(chatRoomRepository.findAllByIdIn(List.of(12L))).willReturn(List.of(room));
        given(studyInfoQueryPort.getStudyInfo(45L)).willReturn(Optional.of(new StudyInfoResult("수학 스터디", "MATH_1")));
        given(chatMessageRepository.findLatestByChatRoomId(12L)).willThrow(new RuntimeException("db down"));

        // when
        List<MyChatRoomDetail> result = chatRoomQueryService.getMyRooms(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("수학 스터디");
        assertThat(result.get(0).lastMessage()).isNull();
        assertThat(result.get(0).lastMessageAt()).isNull();
    }
}
