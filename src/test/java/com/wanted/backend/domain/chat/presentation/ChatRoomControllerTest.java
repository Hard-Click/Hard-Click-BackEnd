package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.result.ChatMessageDetail;
import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;
import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.ParticipantDetail;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageQueryUseCase;
import com.wanted.backend.domain.chat.application.usecase.ChatRoomQueryUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import com.wanted.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomQueryUseCase chatRoomQueryUseCase;

    @MockitoBean
    private ChatMessageQueryUseCase chatMessageQueryUseCase;

    @BeforeEach
    void setUpAuthentication() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "tester", "password", false, true, "USER", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDownAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("참여자가 조회하면 200과 함께 채팅방 상세 정보를 반환한다")
    void getRoom_success() throws Exception {
        ChatRoomDetailResult result = new ChatRoomDetailResult(
                12L, 45L, "수학 1등급 목표 스터디", "MATH_1", 1L, "ACTIVE",
                List.of(new ParticipantDetail(1L, "이*연", true), new ParticipantDetail(2L, "김*수", false)), 2);
        given(chatRoomQueryUseCase.getRoom(eq(12L), eq(1L))).willReturn(result);

        mockMvc.perform(get("/api/chat/rooms/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value(12))
                .andExpect(jsonPath("$.data.groupId").value(45))
                .andExpect(jsonPath("$.data.title").value("수학 1등급 목표 스터디"))
                .andExpect(jsonPath("$.data.subjectName").value("MATH_1"))
                .andExpect(jsonPath("$.data.hostId").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.participantCount").value(2))
                .andExpect(jsonPath("$.data.participants[0].memberId").value(1))
                .andExpect(jsonPath("$.data.participants[0].name").value("이*연"))
                .andExpect(jsonPath("$.data.participants[0].online").value(true))
                .andExpect(jsonPath("$.data.participants[1].memberId").value(2))
                .andExpect(jsonPath("$.data.participants[1].name").value("김*수"))
                .andExpect(jsonPath("$.data.participants[1].online").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 조회하면 404를 반환한다")
    void getRoom_fail_notFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .given(chatRoomQueryUseCase).getRoom(eq(999L), eq(1L));

        mockMvc.perform(get("/api/chat/rooms/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CHAT_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 403을 반환한다")
    void getRoom_fail_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.CHAT_FORBIDDEN))
                .given(chatRoomQueryUseCase).getRoom(eq(12L), eq(1L));

        mockMvc.perform(get("/api/chat/rooms/12"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CHAT_FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("참여자가 조회하면 200과 함께 메시지 목록을 반환한다")
    void getMessages_success() throws Exception {
        ChatMessageListResult result = new ChatMessageListResult(
                List.of(
                        new ChatMessageDetail("SYSTEM_JOIN", 300L, null, null, "김*민님이 입장했습니다",
                                LocalDateTime.of(2026, 7, 7, 20, 59, 0)),
                        new ChatMessageDetail("CHAT", 301L, 2L, "김*민", "안녕하세요!",
                                LocalDateTime.of(2026, 7, 7, 21, 0, 0))),
                true, 300L);
        given(chatMessageQueryUseCase.getMessages(eq(12L), isNull(), eq(20), eq(1L))).willReturn(result);

        mockMvc.perform(get("/api/chat/rooms/12/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].type").value("SYSTEM_JOIN"))
                .andExpect(jsonPath("$.data.messages[0].messageId").value(300))
                .andExpect(jsonPath("$.data.messages[0].senderId").doesNotExist())
                .andExpect(jsonPath("$.data.messages[1].type").value("CHAT"))
                .andExpect(jsonPath("$.data.messages[1].senderId").value(2))
                .andExpect(jsonPath("$.data.messages[1].senderName").value("김*민"))
                .andExpect(jsonPath("$.data.messages[1].content").value("안녕하세요!"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursorId").value(300));
    }

    @Test
    @DisplayName("cursorId와 size 쿼리 파라미터가 use case에 그대로 전달된다")
    void getMessages_success_withCursorAndSize() throws Exception {
        given(chatMessageQueryUseCase.getMessages(eq(12L), eq(300L), eq(10), eq(1L)))
                .willReturn(new ChatMessageListResult(List.of(), false, null));

        mockMvc.perform(get("/api/chat/rooms/12/messages").param("cursorId", "300").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방의 메시지를 조회하면 404를 반환한다")
    void getMessages_fail_notFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .given(chatMessageQueryUseCase).getMessages(eq(999L), any(), anyInt(), eq(1L));

        mockMvc.perform(get("/api/chat/rooms/999/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CHAT_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 메시지를 조회하면 403을 반환한다")
    void getMessages_fail_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.CHAT_FORBIDDEN))
                .given(chatMessageQueryUseCase).getMessages(eq(12L), any(), anyInt(), eq(1L));

        mockMvc.perform(get("/api/chat/rooms/12/messages"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CHAT_FORBIDDEN.getCode()));
    }
}
