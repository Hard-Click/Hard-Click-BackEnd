package com.wanted.backend.domain.churn_management.application.dto;

/**
 * 이탈 사유 축 - Python-Server domain/risk.py 의 RISK_FACTOR_LABELS 와 코드/라벨을 맞춘 것.
 * dropout_risk.features JSON 의 top_reason 코드(recency/streak/quiz)를 화면 라벨로 매핑한다.
 */
public enum ChurnReasonType {
    RECENCY("recency", "장기 미접속"),
    STREAK("streak", "진도 밀림"),
    QUIZ("quiz", "퀴즈 점수 하락"),
    ETC("etc", "기타");

    private final String code;
    private final String label;

    ChurnReasonType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** top_reason 코드(null/미상 포함)를 화면 라벨로. 매칭 실패 시 '기타'. */
    public static String labelOf(String code) {
        if (code == null) {
            return ETC.label;
        }
        for (ChurnReasonType type : values()) {
            if (type.code.equals(code)) {
                return type.label;
            }
        }
        return ETC.label;
    }
}
