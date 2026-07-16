package com.wanted.backend.reviewloop.judge;

import java.util.List;

/** structured output 최상위 — LLM은 findings 배열만 반환한다(점수 없음). */
public record JudgeFindingsDto(List<FindingDto> findings) {}
