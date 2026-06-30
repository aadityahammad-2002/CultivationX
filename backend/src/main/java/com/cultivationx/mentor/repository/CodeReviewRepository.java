package com.cultivationx.mentor.repository;

import com.cultivationx.mentor.entity.CodeReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}