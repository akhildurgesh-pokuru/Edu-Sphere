package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNoAsc(Long quizId, Long studentId);
    List<QuizAttempt> findByQuizIdOrderByStartedAtDesc(Long quizId);
    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndStatus(Long quizId, Long studentId, String status);
    long countByQuizIdAndStudentId(Long quizId, Long studentId);
    List<QuizAttempt> findByStudentIdAndQuizIdIn(Long studentId, Collection<Long> quizIds);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QuizAttempt a where a.id = :id")
    Optional<QuizAttempt> lockById(@Param("id") Long id);
}
