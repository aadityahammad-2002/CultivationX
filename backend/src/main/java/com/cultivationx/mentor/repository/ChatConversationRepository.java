package com.cultivationx.mentor.repository;

import com.cultivationx.mentor.entity.ChatConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findByUserIdAndActiveTrueOrderByLastMessageAtDesc(Long userId, Pageable pageable);

    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) FROM ChatConversation c WHERE c.user.id = :userId AND c.active = true")
    long countActiveByUserId(@Param("userId") Long userId);
}