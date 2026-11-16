package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_grade_history")
@Getter
@Setter
@NoArgsConstructor
public class QuizGradeHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer_id", nullable = false)
    private Long answerId;

    @Column(name = "old_marks")
    private Double oldMarks;

    @Column(name = "new_marks", nullable = false)
    private Double newMarks;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "feedback")
    private String feedback;

    @Column(name = "graded_by", nullable = false)
    private Long gradedBy;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt = Instant.now();
}
