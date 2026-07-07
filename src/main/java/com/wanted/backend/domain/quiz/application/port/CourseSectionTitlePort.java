package com.wanted.backend.domain.quiz.application.port;

import java.util.Collection;
import java.util.Map;

public interface CourseSectionTitlePort {

    Map<Long, String> findTitlesBySectionIds(Collection<Long> sectionIds);
}
