package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    Optional<ProjectMember> findFirstByProjectIdAndRoleAndUserIdNot(
            Long projectId, ProjectRole role, Long userId);

    void deleteAllByUserId(Long userId);
}
