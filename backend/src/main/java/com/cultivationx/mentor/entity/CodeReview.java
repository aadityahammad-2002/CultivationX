package com.cultivationx.mentor.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalCode;

    @Column(nullable = false)
    private String language;

    @Column(columnDefinition = "TEXT")
    private String problemContext;

    // AI Review result
    private Integer overallScore;
    private String grade;

    @Column(columnDefinition = "TEXT")
    private String reviewResultJson;    // full JSON from AI

    @Column(columnDefinition = "TEXT")
    private String betterSolutionJson;  // optimized solution JSON

    @Builder.Default
    private boolean hasBetterSolution = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}