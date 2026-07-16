package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;
import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.MyChatRoomDetail;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageQueryUseCase;
import com.wanted.backend.domain.chat.application.usecase.ChatRoomQueryUseCase;
import com.wanted.backend.domain.chat.presentation.response.ChatMessageListResponse;
import com.wanted.backend.domain.chat.presentation.response.ChatRoomResponse;
import com.wanted.backend.domain.chat.presentation.response.MyChatRoomResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomQueryUseCase chatRoomQueryUseCase;
    private final ChatMessageQueryUseCase chatMessageQueryUseCase;

    public ChatRoomController(ChatRoomQueryUseCase chatRoomQueryUseCase,
                              ChatMessageQueryUseCase chatMessageQueryUseCase) {
        this.chatRoomQueryUseCase = chatRoomQueryUseCase;
        this.chatMessageQueryUseCase = chatMessageQueryUseCase;
    }

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = """
                내가 참여 중인 채팅방 목록을 조회합니다.
                - 마지막 메시지(lastMessage)의 전송 시각(lastMessageAt) 최신순으로 정렬되며, 메시지가 없는 채팅방은 뒤로 밀립니다.
                - unreadCount는 참여자별 마지막으로 읽은 메시지 ID(last_read_message_id) 이후의 메시지 수입니다.
                  해당 채팅방을 WebSocket으로 구독하는 시점에 자동으로 최신 메시지까지 읽음 처리됩니다.
                """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MyChatRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<MyChatRoomDetail> results = chatRoomQueryUseCase.getMyRooms(userDetails.getMemberId());

        return ApiResponse.success("내 채팅방 목록 조회 성공",
                results.stream().map(MyChatRoomResponse::from).toList());
    }

    @Operation(
            summary = "채팅방 정보 조회",
            description = """
                채팅방 상세 정보를 조회합니다.
                - 채팅방 참여자만 조회할 수 있습니다.
                - 스터디 제목/과목명/방장 정보와 참여자별 접속(presence) 여부를 함께 반환합니다.
                """
    )
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId) {

        ChatRoomDetailResult result = chatRoomQueryUseCase.getRoom(chatRoomId, userDetails.getMemberId());

        return ApiResponse.success("채팅방 조회 성공", ChatRoomResponse.from(result));
    }

    @Operation(
            summary = "채팅 메시지 히스토리 조회",
            description = """
                채팅방의 과거 메시지를 커서 기반 페이지네이션으로 조회합니다.
                - 채팅방 참여자만 조회할 수 있습니다.
                - 입장/퇴장 시스템 메시지(SYSTEM_JOIN/SYSTEM_LEAVE)도 함께 반환됩니다.
                - cursorId를 생략하면 최신 메시지부터 조회하며, 응답의 nextCursorId를 다음 요청의 cursorId로 사용하면 이전 메시지를 이어서 조회할 수 있습니다.
                """
    )
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size) {

        ChatMessageListResult result = chatMessageQueryUseCase.getMessages(
                chatRoomId, cursorId, size, userDetails.getMemberId());

        return ApiResponse.success("채팅 메시지 조회 성공", ChatMessageListResponse.from(result));
    }
}
