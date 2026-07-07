package com.wanted.backend.domain.quiz.application.port;

import java.util.Collection;
import java.util.Map;

public interface CourseTitlePort {

    Map<Long, String> findTitlesByCourseIds(Collection<Long> courseIds);
}
