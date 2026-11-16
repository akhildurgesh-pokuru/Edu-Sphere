package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByAttemptId(Long attemptId);
    Optional<QuizAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
    List<QuizAnswer> findByAttemptIdIn(Collection<Long> attemptIds);
}
