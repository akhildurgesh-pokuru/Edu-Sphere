package com.edusphere.repository;

import com.edusphere.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DiscussionReadRepository extends JpaRepository<DiscussionRead, Long> {
    Optional<DiscussionRead> findByDiscussionIdAndUserId(Long discussionId, Long userId);
    List<DiscussionRead> findByUserIdAndDiscussionIdIn(Long userId, Collection<Long> discussionIds);
}
