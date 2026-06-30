package com.cultivationx.rise.repository;

import com.cultivationx.rise.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapRepository extends JpaRepository<SkillGap, Long> {

    Optional<SkillGap> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<SkillGap> findByUserIdOrderByCreatedAtDesc(Long userId);
}