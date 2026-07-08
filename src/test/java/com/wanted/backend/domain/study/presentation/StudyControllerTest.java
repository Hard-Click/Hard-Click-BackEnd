package com.wanted.backend.domain.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.presentation.request.CreateStudyRequest;
import com.wanted.backend.global.domain.SubjectType;
import com.wanted.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StudyController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyCommandUseCase studyCommandUseCase;

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
    @DisplayName("정상 요청 시 201과 함께 스터디/채팅방 ID를 반환한다")
    void createStudy_success() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("제목", SubjectType.MATH_1, 5, "내용");
        given(studyCommandUseCase.create(any())).willReturn(new StudyCreationResult(100L, 200L));

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupId").value(100))
                .andExpect(jsonPath("$.data.chatRoomId").value(200));
    }

    @Test
    @DisplayName("정원이 2명 미만이면 400을 반환한다")
    void createStudy_fail_maxCountTooSmall() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("제목", SubjectType.MATH_1, 1, "내용");

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("제목이 비어 있으면 400을 반환한다")
    void createStudy_fail_blankTitle() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("", SubjectType.MATH_1, 5, "내용");

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("제목이 300자를 초과하면 400을 반환한다")
    void createStudy_fail_titleTooLong() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("a".repeat(301), SubjectType.MATH_1, 5, "내용");

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("과목이 누락되면 400을 반환한다")
    void createStudy_fail_nullSubject() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("제목", null, 5, "내용");

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내용이 비어 있으면 400을 반환한다")
    void createStudy_fail_blankContent() throws Exception {
        CreateStudyRequest request = new CreateStudyRequest("제목", SubjectType.MATH_1, 5, "");

        mockMvc.perform(post("/api/study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
