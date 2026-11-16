package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    @Query("select d from Discussion d where d.subjectId = :subjectId and (:q is null or lower(d.title) like lower(concat('%', :q, '%')) or lower(d.body) like lower(concat('%', :q, '%'))) order by d.pinned desc, d.lastActivityAt desc")
    Page<Discussion> search(@Param("subjectId") Long subjectId, @Param("q") String q, Pageable pageable);
    Optional<Discussion> findByIdAndSubjectId(Long id, Long subjectId);
}
