package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByIdAndTeacherId(Long id, Long teacherId);
    List<Subject> findByTeacherIdOrderByUpdatedAtDesc(Long teacherId);
    @Query("select s from Subject s where s.status = 'PUBLISHED' and (:q is null or lower(s.title) like lower(concat('%', :q, '%')) or lower(coalesce(s.description,'')) like lower(concat('%', :q, '%'))) and (:category is null or s.category = :category)")
    Page<Subject> searchCatalog(@Param("q") String q, @Param("category") String category, Pageable pageable);
    @Query("select distinct s.category from Subject s where s.status = 'PUBLISHED' and s.category is not null order by s.category")
    List<String> publishedCategories();
}
