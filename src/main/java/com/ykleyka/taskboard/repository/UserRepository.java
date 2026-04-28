package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT DISTINCT user
            FROM User user
            WHERE user.id = :currentUserId
               OR user.id IN (
                    SELECT member.user.id
                    FROM ProjectMember member
                    WHERE member.project.id IN (
                        SELECT currentMember.project.id
                        FROM ProjectMember currentMember
                        WHERE currentMember.user.id = :currentUserId
                    )
               )
            """)
    Page<User> findAllVisibleToUser(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("""
            SELECT user
            FROM User user
            WHERE user.id = :id
              AND (
                    user.id = :currentUserId
                    OR user.id IN (
                        SELECT member.user.id
                        FROM ProjectMember member
                        WHERE member.project.id IN (
                            SELECT currentMember.project.id
                            FROM ProjectMember currentMember
                            WHERE currentMember.user.id = :currentUserId
                        )
                    )
              )
            """)
    Optional<User> findVisibleToUserById(
            @Param("id") Long id,
            @Param("currentUserId") Long currentUserId);
}
