package com.wanted.backend.domain.study.presentation.request;

import com.wanted.backend.global.domain.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class StudyListRequest {

    @Schema(description = "과목 필터 (SubjectType enum 값)", example = "MATH_1")
    private SubjectType subject;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Schema(description = "조회 크기", example = "10")
    @Min(value = 1, message = "조회 크기는 1 이상이어야 합니다.")
    @Max(value = 50, message = "조회 크기는 50 이하여야 합니다.")
    private int size = 10;

    public SubjectType getSubject() {
        return subject;
    }

    public void setSubject(SubjectType subject) {
        this.subject = subject;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
