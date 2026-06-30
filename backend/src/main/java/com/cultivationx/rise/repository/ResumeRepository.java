package com.cultivationx.rise.repository;

import com.cultivationx.rise.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByUserIdAndActiveTrue(Long userId);

    List<Resume> findByUserIdOrderByUploadedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE Resume r SET r.active = false WHERE r.user.id = :userId")
    void deactivateAllForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Resume r WHERE r.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    Optional<Resume> findTopByUserIdOrderByUploadedAtDesc(Long userId);
}