package com.cultivationx.auth.repository;

import com.cultivationx.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGithubUsername(String githubUsername);

    Optional<User> findByLeetcodeUsername(String leetcodeUsername);

    @Modifying
    @Query("UPDATE User u SET u.devDnaScore = :score, u.updatedAt = :now WHERE u.id = :userId")
    void updateDevDnaScore(@Param("userId") Long userId, @Param("score") Integer score, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.interviewReadinessScore = :score, u.updatedAt = :now WHERE u.id = :userId")
    void updateInterviewReadiness(@Param("userId") Long userId, @Param("score") Integer score, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.currentStreak = :streak, u.lastActivityDate = :date WHERE u.id = :userId")
    void updateStreak(@Param("userId") Long userId, @Param("streak") Integer streak, @Param("date") LocalDateTime date);
}