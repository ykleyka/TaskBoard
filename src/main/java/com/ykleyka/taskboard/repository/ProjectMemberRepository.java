package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findAllByProjectIdInAndRoleAndUserIdNot(
            Collection<Long> projectIds, ProjectRole role, Long userId);

    void deleteAllByProjectId(Long projectId);

    void deleteAllByUserId(Long userId);
}
