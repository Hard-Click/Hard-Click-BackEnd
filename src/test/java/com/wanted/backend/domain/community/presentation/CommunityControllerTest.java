package com.wanted.backend.domain.community.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.community.application.usecase.CommentCommandUseCase;
import com.wanted.backend.domain.community.application.usecase.CommentQueryUseCase;
import com.wanted.backend.domain.community.application.usecase.PostCommandUseCase;
import com.wanted.backend.domain.community.application.usecase.PostQueryUseCase;
import com.wanted.backend.domain.community.application.usecase.UnifiedBoardQueryUseCase;
import com.wanted.backend.domain.community.application.usecase.ReviewCommandUseCase;
import com.wanted.backend.domain.community.application.usecase.ReviewQueryUseCase;
import com.wanted.backend.domain.community.presentation.request.CreateReviewRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReviewController.class, PostController.class, CommentController.class})
@AutoConfigureMockMvc(addFilters = false)
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private ReviewCommandUseCase reviewCommandUseCase;
    @MockitoBean private ReviewQueryUseCase reviewQueryUseCase;
    @MockitoBean private PostCommandUseCase postCommandUseCase;
    @MockitoBean private PostQueryUseCase postQueryUseCase;
    @MockitoBean private UnifiedBoardQueryUseCase unifiedBoardQueryUseCase;
    @MockitoBean private CommentCommandUseCase commentCommandUseCase;
    @MockitoBean private CommentQueryUseCase commentQueryUseCase;

    @Test
    @DisplayName("별점이 범위를 초과하면 400을 반환한다")
    void createReview_fail_invalidRating() throws Exception {
        // given
        CreateReviewRequest request = new CreateReviewRequest(10, "유익한 강의였습니다.");

        // when & then
        mockMvc.perform(post("/api/courses/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}