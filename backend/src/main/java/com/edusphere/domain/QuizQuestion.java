package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "marks", nullable = false)
    private Double marks = 1.0;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "explanation")
    private String explanation;

    @Column(name = "position", nullable = false)
    private Integer position = 0;
}
