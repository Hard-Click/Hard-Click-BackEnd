package com.wanted.backend.domain.study.application.result;

import java.util.List;

public record StudyListResult(
        List<StudyItemResult> items,
        int totalPages
) {}
