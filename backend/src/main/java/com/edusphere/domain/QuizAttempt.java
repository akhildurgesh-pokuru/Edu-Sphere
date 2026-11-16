package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "status", nullable = false, length = 25)
    private String status = "IN_PROGRESS";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score", nullable = false)
    private Double score = 0.0;

    @Column(name = "max_score", nullable = false)
    private Double maxScore = 0.0;

    @Column(name = "needs_grading", nullable = false)
    private boolean needsGrading;

    @Column(name = "expired", nullable = false)
    private boolean expired;
}
