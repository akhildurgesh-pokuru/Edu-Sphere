package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);
    List<Quiz> findBySubjectIdAndPublishedTrueOrderByCreatedAtDesc(Long subjectId);
}
