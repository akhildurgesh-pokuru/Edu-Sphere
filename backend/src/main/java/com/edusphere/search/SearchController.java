package com.edusphere.search;

import com.edusphere.common.CurrentUser;
import com.edusphere.security.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search")
public class SearchController {
    private final NamedParameterJdbcTemplate jdbc;

    public SearchController(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static String like(String q) {
        return "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    @GetMapping
    @Operation(summary = "Navbar search: teachers get own subjects and students, students get the catalog")
    public Map<String, Object> search(@RequestParam String q) {
        AppUserDetails u = CurrentUser.get();
        Map<String, Object> out = new LinkedHashMap<>();
        if (q == null || q.trim().length() < 2) {
            out.put("subjects", List.of());
            out.put("students", List.of());
            return out;
        }
        MapSqlParameterSource p = new MapSqlParameterSource("q", like(q)).addValue("uid", u.getId());
        if (u.isTeacher()) {
            out.put("subjects", jdbc.queryForList("select id, title, status from subjects where teacher_id = :uid and lower(title) like :q order by title limit 6", p)
                    .stream().map(r -> Map.of("id", r.get("id"), "title", r.get("title"), "status", r.get("status"), "link", "/teacher/subjects/" + r.get("id"))).toList());
            out.put("students", jdbc.queryForList("""
                    select distinct u.id, u.name, u.email, s.title subject, s.id subject_id from enrollments e join users u on u.id = e.student_id
                    join subjects s on s.id = e.subject_id where s.teacher_id = :uid and e.status = 'ACTIVE' and (lower(u.name) like :q or lower(u.email) like :q)
                    order by u.name limit 6""", p).stream().map(r -> Map.of("id", r.get("id"), "name", r.get("name"), "email", r.get("email"),
                    "subject", r.get("subject"), "link", "/teacher/subjects/" + r.get("subject_id") + "?tab=students")).toList());
        } else {
            out.put("subjects", jdbc.queryForList("""
                    select id, title, category from subjects where status = 'PUBLISHED' and (lower(title) like :q or lower(coalesce(category, '')) like :q)
                    order by title limit 6""", p).stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.get("id"));
                m.put("title", r.get("title"));
                m.put("category", r.get("category"));
                m.put("link", "/student/subjects/" + r.get("id"));
                return m;
            }).toList());
            out.put("students", List.of());
        }
        return out;
    }
}
