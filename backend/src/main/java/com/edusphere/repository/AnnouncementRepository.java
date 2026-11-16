package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findBySubjectIdOrderByPinnedDescCreatedAtDesc(Long subjectId);
    List<Announcement> findTop10BySubjectIdInOrderByCreatedAtDesc(Collection<Long> subjectIds);
}
