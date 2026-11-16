# EduSphere build progress

Everything below was built from scratch against the specification, compiled, started against real MySQL and verified.
See [docs/test-report.md](docs/test-report.md) for the exact test results.

| Step | Scope | Status |
|---|---|---|
| 0 | Foundation: Flyway schema (28 tables), auth (register Learn/Teach, login, logout, me, profile, forgot/reset, teacher invitations, providers), security (HttpOnly session, CSRF, CORS, BCrypt(12), rate limit, session rotation, JSON 401/403, headers), optional Google OIDC, bootstrap teacher, subjects and catalog, design system, shell, auth pages, course screens | Done and verified end to end |
| 1 | Modules, lessons, file storage: CRUD + reorder (up/down), lesson types video/text/document/link, draft/published, uploads with content sniffing, random names, `StorageService` interface (local disk), access-checked download with HTTP Range, shared `AccessService` helper | Done |
| 2 | Student learning: curriculum visible to all but locked, player (video, speed, resume, notes, resources, discussion tabs, accordion, previous/next, progress bar), server-side progress with clamped positions, leave course, teacher removes student (REMOVED blocks re-enroll) | Done |
| 3 | Quizzes: builder (4 question types, marks, explanations, schedule, attempts, passing %, randomize, publish), taker (timer, navigator, autosave, confirm), server deadline and attempt limits, auto-grading, manual grading with history, review only when allowed, CSV export | Done |
| 4 | Assignments: create/edit/attachment, submissions view with missing work, grading with feedback; student submit/re-submit until due, backend deadline and file-type enforcement | Done |
| 5 | Discussions (threads, replies, search, delete own, teacher pin/close/moderate, unread indicators) and announcements (create, edit, delete, pin, history, dashboard) | Done |
| 6 | Notifications: bell with unread count, mark read / all, history page, links; types enrollment, new material, new quiz, new assignment, upcoming deadline (hourly job), quiz result, announcement, discussion reply, assignment graded | Done |
| 7 | Analytics: teacher dashboard (Recharts), per-subject analytics, student dashboard and progress page; documented formulas | Done |
| 8 | Search and extras: role-aware navbar search, category filter, cover image upload | Done. Join codes and approval-required enrollment: tables exist, flows intentionally not exposed (optional in the spec) |
| 9 | Hardening and delivery: OpenAPI on every controller, Postman collection (101 requests), ER + architecture + security docs, integration tests, Playwright demo flow, responsive and accessibility pass, final test report | Done |

## Bugs found while building (do not reintroduce)

1. `Module` clashes with `java.lang.Module`; the entity is `CourseModule`.
2. Hibernate validates the schema: SQL types must match JPA types. Enums are stored as plain `String` columns (no `@Enumerated`), long text uses `TEXT` + `@JdbcTypeCode(LONGVARCHAR)`, instants use `DATETIME(6)` with `preferred_instant_jdbc_type=TIMESTAMP`.
3. Never edit a Flyway migration that already ran; add `V2__...sql` for changes.
4. `queryForList` returns `LocalDateTime` for `DATETIME` columns (UTC wall time), not `Timestamp`.
5. Axios across ports needs `withXSRFToken: true`; `lib/api.js` also calls `ensureCsrf()` automatically before every write.
6. `@Transactional` does not apply to self-invocation, so transactional logic sits in separate beans.
7. React Query: do not call `queryClient.clear()` on login/logout, it orphans the `['me']` observer and the UI stays logged out. Remove other queries instead.
8. `SecurityMockMvcRequestPostProcessors.csrf()` swaps the CSRF repository for the whole Spring context; the real cookie test lives in its own context.
9. Local `config/application.yml` outranks `application-test.yml`; test properties are pinned in `@SpringBootTest(properties=…)` and a guard refuses to clean a database that is not `Edu-Sphere-test`.
10. MySQL `now()` follows the server time zone, tests use `utc_timestamp()` because all columns hold UTC.

## Known limits

See "Known limits" in [docs/security.md](docs/security.md) and the test report.
