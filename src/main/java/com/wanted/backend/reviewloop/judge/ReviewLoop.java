package com.wanted.backend.reviewloop.judge;

import java.util.List;

/**
 * Gate 2 한 파일 리뷰의 배선(조립) — 지금까지의 조각을 하나의 흐름으로 잇는다.
 *
 *   rules.yaml(SSOT) → JudgePromptBuilder(정책)
 *                    → LlmJudgePort(LLM findings)
 *                    → EvidenceValidator(환각 제거)
 *                    → JudgeScorer(결정론 점수·라우팅) → JudgeVerdict
 *
 * LlmJudgePort만 갈아끼우면: 테스트=stub, 런타임/Promptfoo=ClaudeJudgeAdapter. 나머지는 그대로.
 * report-only(관찰 모드)로 먼저 붙였다가, 신뢰가 쌓이면 Gate로 승격한다.
 */
public class ReviewLoop implements RoundReviewer {

    private final LlmJudgePort judge;
    private final RuleCatalog catalog;
    private final EvidenceValidator evidenceValidator;
    private final JudgeScorer scorer;
    private final JudgePromptBuilder promptBuilder = new JudgePromptBuilder();

    public ReviewLoop(LlmJudgePort judge,
                      RuleCatalog catalog,
                      EvidenceValidator evidenceValidator,
                      JudgeScorer scorer) {
        this.judge = judge;
        this.catalog = catalog;
        this.evidenceValidator = evidenceValidator;
        this.scorer = scorer;
    }

    @Override
    public JudgeVerdict review(String filePath, String code) {
        String policy = promptBuilder.buildPolicy(catalog);       // rules.yaml에서 조립
        List<Finding> raw = judge.review(filePath, code, policy); // LLM은 findings만
        List<Finding> grounded = evidenceValidator.keepGrounded(raw); // 환각 file:line 제거
        List<Finding> normalized = catalog.normalize(grounded);   // ruleId 화이트리스트 + severity 카탈로그 lookup(LLM 변덕 제거)
        return scorer.score(normalized);                          // 점수·라우팅은 결정론
    }
}
