package com.edusphere.community;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.common.PageResponse;
import com.edusphere.community.CommunityDtos.*;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.repository.*;
import com.edusphere.security.AppUserDetails;
import com.edusphere.subject.AccessService;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-subject forum for the owning teacher and actively enrolled students. User text is stored as plain text and rendered escaped. */
@Service
public class DiscussionService {
    private final DiscussionRepository discussions;
    private final DiscussionReplyRepository replies;
    private final DiscussionReadRepository reads;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;
    private final NamedParameterJdbcTemplate jdbc;

    public DiscussionService(DiscussionRepository discussions, DiscussionReplyRepository replies, DiscussionReadRepository reads,
                             UserRepository users, AccessService access, NotificationService notifications, NamedParameterJdbcTemplate jdbc) {
        this.discussions = discussions; this.replies = replies; this.reads = reads; this.users = users;
        this.access = access; this.notifications = notifications; this.jdbc = jdbc;
    }

    private Map<Long, User> userMap(Collection<Long> ids) {
        return users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private static String roleOf(User u) { return u == null ? "STUDENT" : u.primaryRole(); }

    @Transactional(readOnly = true)
    public PageResponse<ThreadSummary> list(Long subjectId, String q, int page) {
        access.requireAccess(subjectId);
        Long me = CurrentUser.id();
        String query = q == null || q.isBlank() ? null : q.trim();
        var p = discussions.search(subjectId, query, PageRequest.of(Math.max(page, 0), 20));
        List<Discussion> list = p.getContent();
        return PageResponse.of(p, l -> {
            if (l.isEmpty()) return List.<ThreadSummary>of();
            List<Long> ids = l.stream().map(Discussion::getId).toList();
            Map<Long, Long> counts = new HashMap<>();
            jdbc.query("select discussion_id, count(*) c from discussion_replies where discussion_id in (:ids) group by discussion_id",
                    new MapSqlParameterSource("ids", ids), rs -> { counts.put(rs.getLong(1), rs.getLong(2)); });
            Map<Long, DiscussionRead> readBy = reads.findByUserIdAndDiscussionIdIn(me, ids).stream().collect(Collectors.toMap(DiscussionRead::getDiscussionId, r -> r));
            Map<Long, User> authors = userMap(l.stream().map(Discussion::getAuthorId).collect(Collectors.toSet()));
            return l.stream().map(d -> {
                DiscussionRead r = readBy.get(d.getId());
                boolean unread = r == null ? !d.getAuthorId().equals(me) : r.getLastReadAt().isBefore(d.getLastActivityAt());
                User a = authors.get(d.getAuthorId());
                String body = d.getBody();
                return new ThreadSummary(d.getId(), d.getTitle(), body.length() > 160 ? body.substring(0, 160) + "…" : body, d.getAuthorId(),
                        a == null ? "Member" : a.getName(), roleOf(a), counts.getOrDefault(d.getId(), 0L), d.isPinned(), d.isClosed(), unread,
                        d.getCreatedAt(), d.getLastActivityAt());
            }).toList();
        });
    }

    @Transactional
    public ThreadDetail create(Long subjectId, ThreadRequest r) {
        AccessService.Access acc = access.requireAccess(subjectId);
        AppUserDetails u = CurrentUser.get();
        Discussion d = new Discussion();
        d.setSubjectId(subjectId);
        d.setAuthorId(u.getId());
        d.setTitle(r.title().trim());
        d.setBody(r.body().trim());
        d = discussions.save(d);
        markRead(d.getId(), u.getId());
        if (!acc.owner()) {
            notifications.notify(acc.subject().getTeacherId(), "DISCUSSION_REPLY", "New question in " + acc.subject().getTitle(), d.getTitle(),
                    "/teacher/subjects/" + subjectId + "?tab=discussions&thread=" + d.getId(), null);
        }
        return detail(d, acc, u);
    }

    @Transactional
    public ThreadDetail get(Long discussionId) {
        Discussion d = discussions.findById(discussionId).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        AccessService.Access acc = access.requireAccess(d.getSubjectId());
        AppUserDetails u = CurrentUser.get();
        markRead(d.getId(), u.getId());
        return detail(d, acc, u);
    }

    private ThreadDetail detail(Discussion d, AccessService.Access acc, AppUserDetails u) {
        List<DiscussionReply> rs = replies.findByDiscussionIdOrderByCreatedAtAsc(d.getId());
        Set<Long> ids = rs.stream().map(DiscussionReply::getAuthorId).collect(Collectors.toCollection(HashSet::new));
        ids.add(d.getAuthorId());
        Map<Long, User> names = userMap(ids);
        List<ReplyDto> dtos = rs.stream().map(r -> new ReplyDto(r.getId(), r.getAuthorId(), names.containsKey(r.getAuthorId()) ? names.get(r.getAuthorId()).getName() : "Member",
                roleOf(names.get(r.getAuthorId())), r.getBody(), r.getCreatedAt(), acc.owner() || r.getAuthorId().equals(u.getId()))).toList();
        User a = names.get(d.getAuthorId());
        return new ThreadDetail(d.getId(), d.getSubjectId(), d.getTitle(), d.getBody(), d.getAuthorId(), a == null ? "Member" : a.getName(), roleOf(a),
                d.isPinned(), d.isClosed(), d.getCreatedAt(), acc.owner() || d.getAuthorId().equals(u.getId()), acc.owner(), dtos);
    }

    private void markRead(Long discussionId, Long userId) {
        DiscussionRead r = reads.findByDiscussionIdAndUserId(discussionId, userId).orElseGet(() -> {
            DiscussionRead n = new DiscussionRead();
            n.setDiscussionId(discussionId);
            n.setUserId(userId);
            return n;
        });
        r.setLastReadAt(Instant.now());
        reads.save(r);
    }

    @Transactional
    public ThreadDetail reply(Long discussionId, ReplyRequest req) {
        Discussion d = discussions.findById(discussionId).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        AccessService.Access acc = access.requireAccess(d.getSubjectId());
        AppUserDetails u = CurrentUser.get();
        if (d.isClosed() && !acc.owner()) throw ApiException.conflict("This discussion is closed");
        DiscussionReply r = new DiscussionReply();
        r.setDiscussionId(discussionId);
        r.setAuthorId(u.getId());
        r.setBody(req.body().trim());
        replies.save(r);
        d.setLastActivityAt(Instant.now());
        discussions.save(d);
        markRead(discussionId, u.getId());
        Set<Long> targets = new LinkedHashSet<>(List.of(d.getAuthorId(), acc.subject().getTeacherId()));
        targets.remove(u.getId());
        for (Long t : targets) {
            String link = (t.equals(acc.subject().getTeacherId()) ? "/teacher/subjects/" : "/student/subjects/") + d.getSubjectId() + "?tab=discussions&thread=" + d.getId();
            notifications.notify(t, "DISCUSSION_REPLY", u.getName() + " replied: " + d.getTitle(), r.getBody(), link, null);
        }
        return detail(d, acc, u);
    }

    @Transactional
    public void deleteThread(Long id) {
        Discussion d = discussions.findById(id).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        AccessService.Access acc = access.requireAccess(d.getSubjectId());
        if (!acc.owner() && !d.getAuthorId().equals(CurrentUser.id())) throw ApiException.forbidden("You can only delete your own posts");
        discussions.delete(d);
    }

    @Transactional
    public void deleteReply(Long id) {
        DiscussionReply r = replies.findById(id).orElseThrow(() -> ApiException.notFound("Reply not found"));
        Discussion d = discussions.findById(r.getDiscussionId()).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        AccessService.Access acc = access.requireAccess(d.getSubjectId());
        if (!acc.owner() && !r.getAuthorId().equals(CurrentUser.id())) throw ApiException.forbidden("You can only delete your own posts");
        replies.delete(r);
    }

    @Transactional
    public void setPinned(Long id, boolean value) {
        Discussion d = discussions.findById(id).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        access.requireOwnedSubject(d.getSubjectId());
        d.setPinned(value);
        discussions.save(d);
    }

    @Transactional
    public void setClosed(Long id, boolean value) {
        Discussion d = discussions.findById(id).orElseThrow(() -> ApiException.notFound("Discussion not found"));
        access.requireOwnedSubject(d.getSubjectId());
        d.setClosed(value);
        discussions.save(d);
    }
}
