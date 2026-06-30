package com.cultivationx.nexus.repository;

import com.cultivationx.nexus.entity.GitHubProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitHubProfileRepository extends JpaRepository<GitHubProfile, Long> {
    Optional<GitHubProfile> findByUserId(Long userId);
    Optional<GitHubProfile> findByUsername(String username);
}