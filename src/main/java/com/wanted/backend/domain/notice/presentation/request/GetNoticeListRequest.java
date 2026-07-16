package com.wanted.backend.domain.notice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "공지사항 목록 조회 요청")
public class GetNoticeListRequest {

    @Schema(description = "공지사항 타입 (GLOBAL: 전체 공지, COURSE: 강의 공지)", example = "GLOBAL")
    private String type;

    @Schema(description = "강의 공지 조회 시 강의 ID (COURSE 타입일 때 필수)", example = "5")
    private Long courseId;

    @Schema(description = "제목 검색 키워드", example = "업로드")
    private String keyword;

    @Schema(description = "페이지 번호 (0-based)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Schema(description = "페이지당 항목 수", example = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
