package com.wanted.backend.domain.cource.infrastructure.member;

import com.wanted.backend.domain.cource.application.port.CourseAdminCheckPort;
import com.wanted.backend.domain.identity.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAdminCheckAdapter implements CourseAdminCheckPort {

    private final SpringDataCourseMemberReferenceRepository memberReferenceRepository;

    @Override
    public boolean isAdmin(Long memberId) {
        return memberReferenceRepository.findById(memberId)
                .map(member -> member.getRole() == Role.ADMIN)
                .orElse(false);
    }
}
