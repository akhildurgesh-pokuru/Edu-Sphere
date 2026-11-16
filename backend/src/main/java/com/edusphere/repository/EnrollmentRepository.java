package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findBySubjectIdAndStudentId(Long subjectId, Long studentId);
    List<Enrollment> findByStudentIdAndStatus(Long studentId, String status);
    List<Enrollment> findBySubjectIdAndStatus(Long subjectId, String status);
    boolean existsBySubjectIdAndStudentIdAndStatus(Long subjectId, Long studentId, String status);
    @Query("select e.subjectId, count(e) from Enrollment e where e.subjectId in :ids and e.status = 'ACTIVE' group by e.subjectId")
    List<Object[]> countActiveBySubjects(@Param("ids") Collection<Long> ids);
    List<Enrollment> findByStudentIdAndSubjectIdInAndStatus(Long studentId, Collection<Long> subjectIds, String status);
}
