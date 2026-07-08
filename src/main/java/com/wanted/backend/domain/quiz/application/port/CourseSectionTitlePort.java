package com.wanted.backend.domain.quiz.application.port;

import java.util.Collection;
import java.util.Map;

public interface CourseSectionTitlePort {

    Map<Long, String> findTitlesBySectionIds(Collection<Long> sectionIds);

    /** 섹션 id → 섹션 정보(제목 + 주차 순번). 내 퀴즈 목록에서 주차 표시에 사용. */
    Map<Long, SectionInfo> findSectionsByIds(Collection<Long> sectionIds);

    record SectionInfo(String title, int orderIndex) {}
}
