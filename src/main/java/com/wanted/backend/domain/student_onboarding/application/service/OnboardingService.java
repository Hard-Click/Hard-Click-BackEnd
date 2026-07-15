package com.wanted.backend.domain.student_onboarding.application.service;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;
import com.wanted.backend.domain.student_onboarding.application.usecase.OnboardingUseCase;
import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;
import com.wanted.backend.domain.student_onboarding.domain.model.WeeklyAvailability;
import com.wanted.backend.domain.student_onboarding.domain.policy.DailyCapPolicy;
import com.wanted.backend.domain.student_onboarding.domain.policy.GradeCutPolicy;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentAvailabilityJpaEntity;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentAvailabilityJpaRepository;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentCapacityJpaEntity;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentCapacityJpaRepository;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentExamScoreJpaEntity;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentExamScoreJpaRepository;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentProfileJpaEntity;
import com.wanted.backend.domain.student_onboarding.infrastructure.persistence.StudentProfileJpaRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService implements OnboardingUseCase {

    private final StudentProfileJpaRepository profileRepository;
    private final StudentAvailabilityJpaRepository availabilityRepository;
    private final StudentCapacityJpaRepository capacityRepository;
    private final StudentExamScoreJpaRepository examScoreRepository;
    private final Clock clock;

    @Override
    public void saveProfile(Long memberId, OnboardingDtos.SaveProfileCommand command) {
        StudentProfileJpaEntity profile = profileRepository.findById(memberId)
                .orElseGet(() -> StudentProfileJpaEntity.create(memberId));

        profile.apply(
                command.targetUniversity(),
                command.targetMajor(),
                command.admissionStrategy(),
                command.koreanElective(),
                command.mathElective(),
                command.explorationTrack(),
                command.explorationSubject1(),
                command.explorationSubject2(),
                command.secondLanguage(),
                command.studyPreference()
        );
        profileRepository.save(profile);

        markOnboardedIfComplete(memberId);
    }

    @Override
    public void saveAvailability(Long memberId, OnboardingDtos.SaveAvailabilityCommand command) {
        validateSlots(command.unavailableSlotsByDay());

        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(command.unavailableSlotsByDay());
        if (availability.studyDayCount() == 0) {
            throw new BusinessException(ErrorCode.ONBOARDING_NO_AVAILABLE_TIME);
        }

        // 가용시간은 전체 덮어쓰기 - 기존 구간을 지우고 다시 넣는다.
        availabilityRepository.deleteByMemberId(memberId);
        List<StudentAvailabilityJpaEntity> rows = availability.toBlocks().stream()
                .map(b -> StudentAvailabilityJpaEntity.create(
                        memberId, b.dayOfWeek(), b.startTime(), b.endTime()))
                .toList();
        availabilityRepository.saveAll(rows);

        StudentCapacityJpaEntity capacity = capacityRepository.findById(memberId)
                .orElseGet(() -> StudentCapacityJpaEntity.create(memberId));
        capacity.applyAvailability(
                DailyCapPolicy.deriveDailyCapMinutes(availability),
                availability.toRestDaysBitmask()
        );
        capacityRepository.save(capacity);

        markOnboardedIfComplete(memberId);
    }

    @Override
    public void saveExamScores(Long memberId, OnboardingDtos.SaveExamScoresCommand command) {
        LocalDate today = LocalDate.now(clock);
        LocalDate examDate = command.examDate() != null ? command.examDate() : today;
        if (examDate.isAfter(today)) {
            throw new BusinessException(ErrorCode.ONBOARDING_EXAM_DATE_IN_FUTURE);
        }

        Set<SubjectArea> seen = EnumSet.noneOf(SubjectArea.class);
        for (OnboardingDtos.ExamScoreEntry entry : command.scores()) {
            if (!seen.add(entry.subjectArea())) {
                throw new BusinessException(ErrorCode.ONBOARDING_DUPLICATE_SUBJECT_AREA);
            }
        }

        // 같은 응시일로 다시 저장하면 uq_exam_member_area_date 에 걸리므로 선삭제 후 재삽입한다.
        examScoreRepository.deleteByMemberIdAndExamDate(memberId, examDate);

        List<StudentExamScoreJpaEntity> rows = command.scores().stream()
                .map(entry -> StudentExamScoreJpaEntity.create(
                        memberId,
                        entry.subjectArea(),
                        entry.subjectArea().hasElective() ? entry.subjectName() : null,
                        entry.rawScore(),
                        toGrade(entry),
                        examDate
                ))
                .toList();
        examScoreRepository.saveAll(rows);

        markOnboardedIfComplete(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingDtos.OnboardingStatusView getStatus(Long memberId) {
        boolean profileDone = profileRepository.existsById(memberId);
        boolean examDone = examScoreRepository.existsByMemberId(memberId);
        StudentCapacityJpaEntity capacity = capacityRepository.findById(memberId).orElse(null);
        boolean availabilityDone = capacity != null && capacity.getDailyCapMin() != null;

        return new OnboardingDtos.OnboardingStatusView(
                profileDone,
                availabilityDone,
                examDone,
                capacity != null && capacity.isOnboarded(),
                capacity != null ? capacity.getDailyCapMin() : null,
                capacity != null ? capacity.getRestDays() : 0
        );
    }

    private int toGrade(OnboardingDtos.ExamScoreEntry entry) {
        try {
            return GradeCutPolicy.toGrade(entry.subjectArea(), entry.rawScore());
        } catch (IllegalArgumentException e) {
            // 영역마다 만점이 다르다(국어·수학·영어 100 / 한국사·탐구 50) - Bean Validation 으로는
            // 영역별 상한을 걸 수 없어 여기서 도메인 규칙으로 막는다.
            throw new BusinessException(ErrorCode.ONBOARDING_INVALID_RAW_SCORE, e);
        }
    }

    private void validateSlots(Map<Integer, Set<Integer>> unavailableSlotsByDay) {
        for (Map.Entry<Integer, Set<Integer>> entry : unavailableSlotsByDay.entrySet()) {
            Integer day = entry.getKey();
            if (day == null || day < 0 || day >= WeeklyAvailability.DAYS_PER_WEEK) {
                throw new BusinessException(ErrorCode.ONBOARDING_INVALID_DAY_OF_WEEK);
            }
            for (Integer slot : entry.getValue()) {
                if (slot == null || slot < 0 || slot >= WeeklyAvailability.SLOTS_PER_DAY) {
                    throw new BusinessException(ErrorCode.ONBOARDING_INVALID_TIME_SLOT);
                }
            }
        }
    }

    /**
     * 3단계가 모두 저장됐으면 온보딩 완료로 표시한다.
     *
     * <p>단계 순서를 강제하지 않는 이유: 화면은 순서대로 진행하지만 사용자가 중간에 이탈했다
     * 되돌아올 수 있고, 각 단계가 독립적인 덮어쓰기라 순서에 의존할 필요가 없다.
     * 완료 판정만 세 단계가 다 찼는지로 본다.
     */
    private void markOnboardedIfComplete(Long memberId) {
        StudentCapacityJpaEntity capacity = capacityRepository.findById(memberId).orElse(null);
        if (capacity == null || capacity.getDailyCapMin() == null || capacity.isOnboarded()) {
            return;
        }
        if (!profileRepository.existsById(memberId) || !examScoreRepository.existsByMemberId(memberId)) {
            return;
        }
        capacity.markOnboarded(LocalDateTime.now(clock));
        capacityRepository.save(capacity);
    }
}
