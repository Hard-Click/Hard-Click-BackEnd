package com.wanted.backend.domain.notice.presentation;

import com.wanted.backend.domain.notice.application.result.NoticeListResult;
import com.wanted.backend.domain.notice.application.usecase.NoticeCommandUseCase;
import com.wanted.backend.domain.notice.application.usecase.NoticeQueryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeCommandUseCase noticeCommandUseCase;

    @MockitoBean
    private NoticeQueryUseCase noticeQueryUseCase;

    @Test
    void getNotices_fail_sizeExceedsMax() throws Exception {
        mockMvc.perform(get("/api/notices").param("type", "GLOBAL").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNotices_fail_negativePage() throws Exception {
        mockMvc.perform(get("/api/notices").param("type", "GLOBAL").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNotices_success_withinBounds() throws Exception {
        when(noticeQueryUseCase.getList(any())).thenReturn(new NoticeListResult(List.of(), 0));

        mockMvc.perform(get("/api/notices").param("type", "GLOBAL").param("size", "50"))
                .andExpect(status().isOk());
    }
}
