package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "assignment_submissions")
@Getter
@Setter
@NoArgsConstructor
public class AssignmentSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "late", nullable = false)
    private boolean late;

    @Column(name = "marks")
    private Double marks;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "feedback")
    private String feedback;

    @Column(name = "graded_at")
    private Instant gradedAt;
}
