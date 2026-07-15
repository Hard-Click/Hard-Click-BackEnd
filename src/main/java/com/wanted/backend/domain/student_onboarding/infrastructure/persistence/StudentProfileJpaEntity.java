package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import com.wanted.backend.domain.student_onboarding.domain.model.AdmissionStrategy;
import com.wanted.backend.domain.student_onboarding.domain.model.ExplorationTrack;
import com.wanted.backend.domain.student_onboarding.domain.model.StudyPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** student_profile (V3.2.1) - 온보딩 1단계 입력값. PK = member_id (학생 1:1). */
@Entity
@Getter
@Table(name = "student_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfileJpaEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "target_university", length = 100)
    private String targetUniversity;

    @Column(name = "target_major", length = 100)
    private String targetMajor;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_strategy", nullable = false, length = 20)
    private AdmissionStrategy admissionStrategy;

    @Column(name = "korean_elective", length = 30)
    private String koreanElective;

    @Column(name = "math_elective", length = 30)
    private String mathElective;

    @Enumerated(EnumType.STRING)
    @Column(name = "exploration_track", length = 20)
    private ExplorationTrack explorationTrack;

    @Column(name = "exploration_subject_1", length = 40)
    private String explorationSubject1;

    @Column(name = "exploration_subject_2", length = 40)
    private String explorationSubject2;

    @Column(name = "second_language", nullable = false)
    private boolean secondLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_preference", nullable = false, length = 20)
    private StudyPreference studyPreference;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static StudentProfileJpaEntity create(Long memberId) {
        StudentProfileJpaEntity e = new StudentProfileJpaEntity();
        e.memberId = memberId;
        return e;
    }

    /** 온보딩은 '다음' 버튼으로 되돌아와 다시 저장할 수 있으므로 전체 덮어쓰기(upsert)로 갱신한다. */
    public void apply(
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
        this.targetUniversity = targetUniversity;
        this.targetMajor = targetMajor;
        this.admissionStrategy = admissionStrategy;
        this.koreanElective = koreanElective;
        this.mathElective = mathElective;
        this.explorationTrack = explorationTrack;
        this.explorationSubject1 = explorationSubject1;
        this.explorationSubject2 = explorationSubject2;
        this.secondLanguage = secondLanguage;
        this.studyPreference = studyPreference;
    }
}
