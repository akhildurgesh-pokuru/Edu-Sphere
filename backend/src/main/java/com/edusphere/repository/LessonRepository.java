package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModuleIdOrderByPositionAscIdAsc(Long moduleId);
    List<Lesson> findBySubjectIdOrderByPositionAscIdAsc(Long subjectId);
    List<Lesson> findByResourceId(Long resourceId);
    long countBySubjectId(Long subjectId);
    List<Lesson> findBySubjectIdAndPublishedTrue(Long subjectId);
    long countBySubjectIdAndPublishedTrue(Long subjectId);
}
