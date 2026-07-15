package com.wanted.backend.domain.study_schedule.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudentTodoJpaRepository extends JpaRepository<StudentTodoJpaEntity, Long> {

    List<StudentTodoJpaEntity> findByMemberIdAndPlanDateBetween(Long memberId, LocalDate from, LocalDate to);
}
