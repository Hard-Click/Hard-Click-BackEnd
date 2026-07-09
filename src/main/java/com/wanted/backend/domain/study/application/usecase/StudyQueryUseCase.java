package com.wanted.backend.domain.study.application.usecase;

import com.wanted.backend.domain.study.application.result.StudyDetailResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;

public interface StudyQueryUseCase {
    StudyListResult getList(String subject, int page, int size);

    StudyDetailResult getDetail(Long groupId, Long memberId);
}
