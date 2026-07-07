package com.wanted.backend.domain.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.presentation.request.CreateStudyRequest;
import com.wanted.backend.global.domain.SubjectType;
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

@WebMvcTest(controllers = StudyController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyCommandUseCase studyCommandUseCase;

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
}
