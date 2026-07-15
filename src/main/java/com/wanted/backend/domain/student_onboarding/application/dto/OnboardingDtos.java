package com.wanted.backend.domain.student_onboarding.application.dto;

import com.wanted.backend.domain.student_onboarding.domain.model.AdmissionStrategy;
import com.wanted.backend.domain.student_onboarding.domain.model.ExplorationTrack;
import com.wanted.backend.domain.student_onboarding.domain.model.StudyPreference;
import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 온보딩 application 레이어 입출력 DTO. */
public final class OnboardingDtos {

    private OnboardingDtos() {
    }

    /** 1단계 - 학습 스케줄 초기 설정. */
    public record SaveProfileCommand(
            String targetUniversity,
            String targetMajor,
            AdmissionStrategy admissionStrategy,
            String koreanElective,
            String mathElective,
            ExplorationTrack explorationTrack,
            String explorationSubject1,
            String explorationSubject2,
            boolean secondLanguage,
            StudyPreference studyPreference
    ) {
    }

    /**
     * 2단계 - 불가능한 시간 체크.
     *
     * @param unavailableSlotsByDay 요일(0=일~6=토) -> 불가능한 30분 슬롯 인덱스(0~47). 없는 요일 = 종일 가능.
     */
    public record SaveAvailabilityCommand(
            Map<Integer, Set<Integer>> unavailableSlotsByDay
    ) {
    }

    /** 3단계 - 최근 모의고사 성적. */
    public record SaveExamScoresCommand(
            LocalDate examDate,
            List<ExamScoreEntry> scores
    ) {
    }

    /** 응시영역 하나의 성적. subjectName 은 선택과목이 없는 영역(영어/한국사)에선 null. */
    public record ExamScoreEntry(
            SubjectArea subjectArea,
            String subjectName,
            int rawScore
    ) {
    }

    /**
     * 온보딩 진행 상태 - 재진입/이어하기용.
     *
     * @param dailyCapMin 가용시간에서 유도된 하루 학습 상한(분). 2단계 전이면 null.
     * @param restDays    휴식 요일 비트마스크. 2단계 전이면 0.
     */
    public record OnboardingStatusView(
            boolean profileCompleted,
            boolean availabilityCompleted,
            boolean examScoreCompleted,
            boolean onboarded,
            Integer dailyCapMin,
            int restDays
    ) {
    }
}
