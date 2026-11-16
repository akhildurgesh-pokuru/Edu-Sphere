# Security overview

| Area | What is implemented | Where |
|---|---|---|
| Authentication | Server session in an `HttpOnly`, `SameSite=Lax` cookie (`Secure` via `COOKIE_SECURE`). Session id is rotated at login. Nothing secret in `localStorage` (only the theme). | `AuthController`, `SecurityConfig`, `application.yml` |
| Passwords | BCrypt(12). Policy: 8-72 chars, at least a letter and a number. Reset tokens are random, single-use, expire in 1 h and are stored only as SHA-256 hashes. | `SecurityConfig`, `AuthService`, `TokenUtil` |
| Brute force | Login: 5 failures per IP+email per 15 min, then HTTP 429. Forgot-password is limited per IP. Generic "Invalid email or password" for unknown user and wrong password. | `LoginRateLimiter` |
| CSRF | Cookie token + `X-XSRF-TOKEN` header on every write. Axios sends it (`withXSRFToken`), `ensureCsrf()` runs before writes. | `SecurityConfig`, `lib/api.js` |
| CORS | Pinned to `APP_FRONTEND_URL` with credentials. | `SecurityConfig` |
| Headers | `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, CSP `default-src 'none'` on API JSON, `Cache-Control: no-store` on API responses, HSTS when `COOKIE_SECURE=true`. | `SecurityConfig`, `SecurityHeadersFilter` |
| Authorization | Role gate in `SecurityConfig`, then **every service re-checks** ownership or active enrollment via `AccessService`. Frontend guards are convenience only. | `AccessService` and all services |
| IDOR | Other teachers' subjects, quizzes, attempts, assignments, submissions and files return 404. Students never reach lessons, files, quiz answers or discussions of subjects they are not enrolled in, nor draft lessons. Covered by integration tests that call the API with foreign ids. | `*IntegrationTest` |
| Roles | TEACHER accounts only through sign-up (`ALLOW_TEACHER_SIGNUP`), a teacher invitation, or `BOOTSTRAP_TEACHER_*`. Unknown roles become STUDENT. Google sign-in only ever creates STUDENTs. | `AuthService`, `GoogleAccountService` |
| Google OIDC | Verified email only; linking an existing local account clears its (never email-verified) password so it cannot be pre-hijacked. Password login works without any Google setup. | `GoogleAccountService` |
| Uploads | Extension whitelist, size limits per type, magic-byte check of the real content, random server-chosen names, path normalisation, files `rw-------`, stored outside the source tree and never served statically. Downloads only through the access-checked endpoint (Range supported); non-inline types are forced to `attachment` with `nosniff`. | `FileValidator`, `LocalStorageService`, `ResourceService` |
| Quizzes | Deadline computed on the server; late answers ignored; attempt limit and window checked on the server; correct answers/explanations never sent before review is allowed; duplicate submit returns 409; row lock on attempts prevents double submission. | `AttemptService` |
| Injection | Spring Data / parameterized JDBC only. Search input is escaped for `LIKE`. Bean Validation on every DTO. | all repositories |
| XSS | The API stores user text as plain text; React renders it escaped (no `dangerouslySetInnerHTML` anywhere). Tested end to end with a hostile payload. | frontend, `demo-flow.spec.js` |
| CSV injection | Exported CSV cells starting with `= + - @` are prefixed with `'`. | `QuizService.csv` |
| Errors | One `ApiError` shape. No stack traces, SQL or secrets in responses or logs. | `GlobalExceptionHandler` |
| Secrets | Real values only in environment variables or the git-ignored `backend/config/application.yml`. `.env.example` files are committed. | `.gitignore` |

## Known limits (honest list)

* The rate limiter is in memory (one instance). Use Redis or a gateway for a multi-node deployment.
* Password reset links are only sent by email and never logged. Without SMTP configured (`MAIL_ENABLED=false`) no reset email is delivered.
* Uploaded files are not virus scanned.
* Join codes / approval-required enrollment tables exist but the flows are not exposed (enrollment type is OPEN).
