package com.cultivationx.devdna.repository;

import com.cultivationx.devdna.entity.DevDnaReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DevDnaReportRepository extends JpaRepository<DevDnaReport, Long> {

    Optional<DevDnaReport> findTopByUserIdOrderByGeneratedAtDesc(Long userId);

    List<DevDnaReport> findByUserIdOrderByGeneratedAtDesc(Long userId, Pageable pageable);
}