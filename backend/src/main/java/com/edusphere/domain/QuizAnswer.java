package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
public class QuizAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_ids", length = 500)
    private String selectedOptionIds;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "text_answer")
    private String textAnswer;

    @Column(name = "marks_awarded")
    private Double marksAwarded;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "feedback")
    private String feedback;

    @Column(name = "graded_by")
    private Long gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;
}
