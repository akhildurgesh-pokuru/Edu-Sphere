# EduSphere final test report

All results below were produced by actually running the commands on macOS (JDK 17, Maven 3.10, Node 24, MySQL 8.0.42, Chromium via Playwright).

| Suite | Command | Result |
|---|---|---|
| Backend unit + integration | `cd backend && mvn test` | **82 tests, 0 failures, 0 errors** |
| Frontend unit / component | `cd frontend && npm test` | **46 tests in 8 files, all passed** |
| Frontend production build | `cd frontend && npm run build` | **built, no warnings** (largest chunk 398 kB, Recharts) |
| End-to-end (Playwright, real browser + real backend + MySQL) | `cd frontend && npm run e2e` | **5 of 5 passed** (~19 s) |
| Backend start against real MySQL | `mvn spring-boot:run` | Flyway applied the migration, Hibernate `validate` passed, health `UP` |
| OpenAPI | `GET /v3/api-docs` | 79 paths, 13 tags, every operation has a summary; Swagger UI served at `/swagger-ui.html` |

## Backend (82)

| Class | Tests | Covers |
|---|---|---|
| `AuthIntegrationTest` | 15 | registration validation and field errors, role assignment (unknown role becomes STUDENT), duplicate email (case-insensitive), BCrypt hash stored, login failure message, rate limit (429 after 5), unauthenticated access (401), CSRF missing (403), logout invalidates session, single-use and expired reset tokens, teacher invitation rules |
| `CsrfCookieIntegrationTest` | 2 | CSRF cookie is script-readable + `SameSite=Lax`; write succeeds only when header equals cookie |
| `SubjectAccessIntegrationTest` | 12 | role gates (student vs teacher APIs), draft subjects invisible, publish rules, **teacher A cannot read/update/publish/curriculum/students/modules of teacher B (404)**, enrollment + duplicate (409), **lessons / discussions / announcements / quizzes / assignments / progress locked until enrolled (403) even with edited ids**, draft lessons hidden, leave and REMOVED re-enroll block (403), SQL-injection style catalog search, paging, server-side progress and position clamping, reorder validation |
| `FileAccessIntegrationTest` | 7 | content sniffing (script renamed `.pdf`, `.exe`, `.sh`, fake MP4, empty file rejected), random stored names / path traversal in filename, owner + enrolled-only downloads (401/404), draft-lesson and orphan files hidden, **HTTP Range 206 + `Content-Range`**, cross-subject file reuse blocked, cover image validation and visibility |
| `QuizIntegrationTest` | 8 | full flow with auto + manual grading, **correct answers never sent early**, server-computed deadline, resume, duplicate submit (409), late save (409), grading history, attempt limit (409), expired attempts keep saved answers only, stalled attempt auto-submit, other students / teachers cannot reach attempts, question validation and lock after attempts, closed / future quizzes, CSV formula injection |
| `CommunityAndAssignmentIntegrationTest` | 7 | assignment deadline + file types + resubmit, missing-work view, grading limits, file access for submissions, discussions limited to owner + enrolled, **hostile `<script>` text stored and returned as data**, unread indicator, moderation, announcements and notifications ownership, dashboards/analytics numbers from stored activity, role-aware search |
| `MigrationIntegrationTest` | 3 | every table exists after Flyway, roles seeded, unique constraints exist, **database itself rejects a duplicate enrollment** |
| `AuthServiceTest` (Mockito) | 9 | register rules, teacher sign-up switch, invitation validation, hashed reset tokens |
| `ProgressServiceTest` (Mockito) | 8 | progress = completed / published, clamping, furthest position, drafts untracked |
| `FileValidatorTest`, `LocalStorageServiceTest`, `QuizServiceUnitTest` | 11 | whitelist, magic bytes, name sanitising, storage path safety, CSV escaping, percent maths |

Integration tests run against their own database `Edu-Sphere-test` (rebuilt from the migrations on every run). A guard refuses to clean any other
database, and the test URL is pinned in `@SpringBootTest(properties=…)` so a local `config/application.yml` can never redirect them. (This guard
did fire once during development when a local override pointed the test at the real database, and it prevented the clean.)

## Frontend (46)

* `schemas.test.js` Zod rules for login, register, reset, subject, quiz, assignment
* `api.test.js` `parseError` kinds and `applyFieldErrors` mapping backend field errors onto forms
* `utils.test.js` duration, bytes, initials, plurals, stable gradients
* `states.test.jsx` empty / error / forbidden states, `QueryBoundary` loading, error, empty and data, `StatCard` skeleton
* `routes.test.jsx` route protection: anonymous redirect, loading state, role guard, session-check error, public-only pages
* `LoginPage.test.jsx`, `RegisterPage.test.jsx` validation messages, submit payloads (Learn / Teach), teacher sign-up switched off, server errors, show password
* `SubjectCard.test.jsx` card content, progress bar, status badge

## End to end (Playwright, 5)

1. **Teacher demo flow**: registers with *Teach*, sees the empty dashboard, creates "Java Programming", adds three modules, uploads a video and a PDF with progress UI, adds a text lesson, builds a **10-question quiz** (9 single choice + 1 short answer), publishes it, creates an assignment, posts an announcement, publishes the subject.
2. **Student demo flow**: registers with *Learn*, searches the catalog, enrolls, opens the player (video element, mark complete), **downloads the PDF** (filename checked), takes the timed quiz with the question navigator and submits, sees the pending-grading result and review, submits an assignment, posts a forum question containing `<img onerror>` (**shown as text, never executed**), opens the progress page.
3. **Teacher follow-up**: sees the new enrollment on the dashboard, grades the submission (92/100), grades the short answer, reads the analytics.
4. **Security**: the student is redirected from teacher pages and the API answers 403 to direct calls.
5. **Mobile (375 px)**: no horizontal overflow on dashboard, subjects, notifications and profile; the sidebar opens as a drawer.

## Security checks executed

Authentication bypass (401 on every protected route), privilege escalation (student to teacher APIs and invitations: 403), insecure direct object references (foreign ids on subjects, lessons, files, quizzes, attempts, submissions, discussions, notifications: 403/404), SQL injection payloads in catalog and navbar search (no error, no rows), XSS payload in the forum (stored verbatim, rendered escaped, verified in the browser), CSRF (missing and wrong header: 403), upload restrictions (type, size class, content mismatch, traversal), rate limiting (login 429), session invalidation (logout, reset-password), spreadsheet formula injection in CSV export.

## What is still limited (honest list)

* **Google sign-in** is implemented and unit-structured, but was **not tested against real Google credentials** (none available). Password login is fully tested.
* **Video playback** was verified with a header-only MP4 fixture. Range streaming is tested at the HTTP level (206, `Content-Range`), and the resume position is saved/clamped by the API (tested), but resuming playback of a real, decodable video in a browser was not exercised.
* **Quiz timer reaching zero in the browser** (auto-submit) is covered at API level (expired attempt closes with saved answers only), not by a UI test that waits for the clock.
* **Join codes and approval-required enrollment**: tables exist, flows are intentionally not exposed (the specification marks them optional). Enrollment type is OPEN.
* **Email**: needs `MAIL_ENABLED=true` and SMTP settings. Delivery was verified against a local test SMTP server only, not a real provider.
* **Accessibility** was reviewed by hand (labels, focus rings, skip link, `aria-live`, table captions, keyboard dialogs) but no automated axe audit was run. Responsive checks cover the four pages listed above at 375 px.
* **Performance / load** was not tested. The login rate limiter is in memory (single node).
* The Playwright flow creates `*@e2e.local` accounts in the backend's database; remove them with `frontend/e2e/cleanup-e2e-data.sql`.
* Uploaded files are not virus scanned.
