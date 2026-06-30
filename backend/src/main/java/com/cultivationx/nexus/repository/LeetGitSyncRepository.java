package com.cultivationx.nexus.repository;

import com.cultivationx.nexus.entity.LeetGitSync;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeetGitSyncRepository extends JpaRepository<LeetGitSync, Long> {

    List<LeetGitSync> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<LeetGitSync> findByUserIdAndProblemSlug(Long userId, String problemSlug);

    @Query("SELECT COUNT(l) FROM LeetGitSync l WHERE l.user.id = :userId AND l.status = 'SYNCED'")
    long countSyncedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM LeetGitSync l WHERE l.user.id = :userId")
    long countTotalByUserId(@Param("userId") Long userId);
}