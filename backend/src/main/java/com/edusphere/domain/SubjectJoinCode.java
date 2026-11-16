package com.edusphere.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "subject_join_codes")
@Getter
@Setter
@NoArgsConstructor
public class SubjectJoinCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
