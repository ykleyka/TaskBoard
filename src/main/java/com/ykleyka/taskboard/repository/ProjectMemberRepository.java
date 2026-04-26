package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findAllByProjectId(Long projectId);

    List<ProjectMember> findAllByProjectIdInAndRoleAndUserIdNot(
            Collection<Long> projectIds, ProjectRole role, Long userId);

    List<ProjectMember> findAllByUserIdAndRole(Long userId, ProjectRole role);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectIdAndRole(Long projectId, ProjectRole role);

    @Query("""
            SELECT COUNT(DISTINCT member.user.id)
            FROM ProjectMember member
            JOIN member.project project
            JOIN project.members currentMember
            WHERE currentMember.user.id = :userId
              AND member.user.id <> :userId
            """)
    long countCollaboratorsVisibleToUser(@Param("userId") Long userId);

    void deleteAllByProjectId(Long projectId);

    void deleteAllByUserId(Long userId);
}
