package com.wanted.backend.domain.study.application.usecase;

import com.wanted.backend.domain.study.application.result.StudyDetailResult;

public interface StudyQueryUseCase {
    StudyDetailResult getDetail(Long groupId, Long memberId);
}
