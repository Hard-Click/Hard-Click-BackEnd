package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** student_exam_score (V3.2.2) - 온보딩 3단계 입력값. 응시영역 단위 원점수 + 변환된 등급. */
@Entity
@Getter
@Table(name = "student_exam_score")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentExamScoreJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_area", nullable = false, length = 20)
    private SubjectArea subjectArea;

    @Column(name = "subject_name", length = 40)
    private String subjectName;

    @Column(name = "raw_score", nullable = false)
    private Short rawScore;

    @Column(name = "grade", nullable = false)
    private Byte grade;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static StudentExamScoreJpaEntity create(
            Long memberId,
            SubjectArea subjectArea,
            String subjectName,
            int rawScore,
            int grade,
            LocalDate examDate
    ) {
        StudentExamScoreJpaEntity e = new StudentExamScoreJpaEntity();
        e.memberId = memberId;
        e.subjectArea = subjectArea;
        e.subjectName = subjectName;
        e.rawScore = (short) rawScore;
        e.grade = (byte) grade;
        e.examDate = examDate;
        return e;
    }
}
