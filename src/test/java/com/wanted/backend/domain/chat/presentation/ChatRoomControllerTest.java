package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.ParticipantDetail;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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
}
