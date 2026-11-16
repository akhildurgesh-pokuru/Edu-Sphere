# EduSphere

A full-stack e-learning platform for a college demonstration. Teachers publish courses (called *subjects*), students browse one
shared catalog and enroll in one click, like Udemy. Everything is stored in MySQL and every number on screen comes from the backend.

* **Frontend** React 18, Vite, Tailwind CSS 3, Radix primitives, React Router 6, TanStack Query 5, React Hook Form + Zod, Recharts, Sonner, Framer Motion, Lucide
* **Backend** Java 17, Spring Boot 3.5.0 (Web MVC, Data JPA, Security, Validation, OAuth2 Client, Actuator), springdoc OpenAPI 2.8.6, Flyway, Maven
* **Database** MySQL 8, database **`Edu-Sphere-database`** (created automatically on first start)

## What it does

| Teacher | Student |
|---|---|
| Register with **Teach**, create subjects, modules and lessons (video, text, document, link), reorder, publish | Register with **Learn**, search the catalog, filter by category, enroll in one click |
| Upload MP4/WebM, PDF, PPT/PPTX, DOC/DOCX with progress bars; cover image upload | Learning player: HTML5 video, speed control, resume where you left off, notes, resources, discussion, curriculum accordion, previous/next |
| Quizzes: single choice, multiple select, true/false, short answer, timer, attempts, passing %, schedule, randomize, CSV export, manual grading | Timed quizzes with question navigator, autosave, confirm before submit, result and review (when allowed) |
| Assignments with deadline, file types, attachment, marks and feedback, missing-work view | Submit one file (re-submit until due), see status, marks and feedback |
| Announcements, forum moderation (pin, close, delete) | Per-course forum with unread indicators; announcements on the dashboard |
| Dashboard with Recharts, per-subject analytics, students needing support | Dashboard and progress page with real completion, scores and deadlines |
| Navbar search over own subjects and students, notification bell | Navbar search over the catalog, notification bell |

Lesson content, files, quizzes and discussions stay **locked on the server** until a student has an active enrollment, even if ids are edited in requests.

## Quick start (macOS / Linux)

Prerequisites: JDK 17, Maven 3.9+, Node 20+, MySQL 8 running on `localhost:3306`.

### 1. Backend

```bash
cd backend
# Either export the variables ...
export DB_PASSWORD='your-mysql-password'
# ... or copy the example config (git-ignored) and edit it:
cp config/application.yml.example config/application.yml

mvn spring-boot:run
```

Flyway creates all tables on the first start. The API runs on <http://localhost:8080>, Swagger UI at <http://localhost:8080/swagger-ui.html>.
(Use `export`, not `set`, on macOS.)

### 2. Frontend (second terminal)

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open <http://localhost:5173>.

### Demo accounts

| Role | Email | Password | How |
|---|---|---|---|
| Teacher | `teacher@edusphere.local` | `Teacher@12345` | Created at startup from `app.bootstrap-teacher` in `backend/config/application.yml` (or `BOOTSTRAP_TEACHER_*` variables). Not created if unset. |
| Student / Teacher | any | any | Register on the sign-up page and choose **Learn** or **Teach**. |

There is no seeded or fake course data: register, create "Java Programming", and everything on the dashboards is computed from what you do.

## Configuration

Never commit real values. `.env.example` files are shipped; real values go in environment variables or the git-ignored `backend/config/application.yml`.

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | `jdbc:mysql://localhost:3306/Edu-Sphere-database?createDatabaseIfNotExist=true…`, `root`, empty |
| `APP_FRONTEND_URL`, `APP_BACKEND_URL` | CORS origin and links | `http://localhost:5173`, `http://localhost:8080` |
| `STORAGE_PATH` | Upload folder (outside the source tree) | `./uploads` |
| `GOOGLE_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Optional Google sign-in | disabled |
| `ALLOW_TEACHER_SIGNUP` | Let people pick Teacher when registering. `false` = invitation only | `true` |
| `BOOTSTRAP_TEACHER_EMAIL`, `_PASSWORD`, `_NAME` | First teacher created at startup if set | unset |
| `MAIL_ENABLED` | Send real emails (welcome, reset, notifications, teacher invitations) | `false` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server and login | `localhost`, `587` |
| `MAIL_FROM`, `MAIL_FROM_NAME` | Sender address and display name | `no-reply@edusphere.local`, `EduSphere` |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | Turn off for a local test SMTP server | `true` |
| `COOKIE_SECURE` | Set `true` behind HTTPS | `false` |
| `VITE_API_BASE_URL` (frontend) | Backend API base | `http://localhost:8080/api` |

### Optional: Google sign-in

Password login works with no Google setup. To enable Google:

1. In [Google Cloud Console](https://console.cloud.google.com/) create an **OAuth client ID** of type *Web application*.
2. Add the authorised redirect URI `http://localhost:8080/login/oauth2/code/google`.
3. Start the backend with `GOOGLE_ENABLED=true GOOGLE_CLIENT_ID=… GOOGLE_CLIENT_SECRET=…`.
4. The login and register pages show **Continue with Google**. Google always opens its account chooser, so the user picks which account to use.
   Without these settings the button is still shown, but explains that Google sign-in is not set up yet.

Google sign-in only accepts verified emails and **always creates a STUDENT**, never a teacher. Linking an existing local account
removes its (never email-verified) password so it cannot be hijacked.

### Teacher accounts

Teachers come from (a) the sign-up page while `ALLOW_TEACHER_SIGNUP=true`, (b) a teacher invitation (`POST /api/teacher/invitations`
returns a single-use link, valid 7 days, bound to one email), or (c) the bootstrap variables.

### Password reset

The single-use reset link (valid 1 hour) is emailed to the user. A confirmation email is sent after the password changes.
The response is identical for unknown addresses, so accounts cannot be discovered. Links are never written to the log.

### Email

Set `MAIL_ENABLED=true` plus the SMTP settings to turn on:
* a **welcome email** after registration (student, teacher, or teacher invitation),
* the **password reset** and **password changed** emails,
* the **teacher invitation** email,
* an email copy of every **in-app notification** (new lesson, quiz, assignment, grades, announcements, deadline reminders).
  Each user can switch these activity emails off on the Profile page; security emails are always sent.

Gmail example: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME=you@gmail.com`, and `MAIL_PASSWORD` set to a Google
**App Password** (Google Account > Security > 2-Step Verification > App passwords), and `MAIL_FROM=you@gmail.com`.
Other free options: Brevo (300 emails/day), Mailtrap (testing inbox), Resend.
Emails are sent in the background after the database change commits, so a slow or failing SMTP server never breaks a request;
failures are logged. With `MAIL_ENABLED=false` nothing is sent.

## Tests

```bash
cd backend && mvn test                 # unit (Mockito) + integration (Spring Boot Test, MockMvc) against MySQL database Edu-Sphere-test
cd frontend && npm test                # Vitest + React Testing Library
cd frontend && npm run build           # production build
cd frontend && npx playwright install chromium   # once
cd frontend && npm run e2e             # Playwright demo flow (starts/reuses both servers)
```

Integration tests use their own database `Edu-Sphere-test`, rebuilt from the Flyway migrations on each run; a guard refuses to clean any
other database. See [docs/test-report.md](docs/test-report.md) for what passed and what is still limited.

## API

* Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs`
* Postman: [postman/EduSphere.postman_collection.json](postman/EduSphere.postman_collection.json) (log in first; a collection script adds the CSRF header)
* All writes need the `X-XSRF-TOKEN` header equal to the `XSRF-TOKEN` cookie (`GET /api/auth/csrf` sets it). The frontend does this automatically.

## Documentation

* [docs/architecture.md](docs/architecture.md) architecture diagram and layer rules
* [docs/er-diagram.md](docs/er-diagram.md) ER diagram and database constraints
* [docs/security.md](docs/security.md) security overview and known limits
* [docs/analytics-formulas.md](docs/analytics-formulas.md) how every dashboard number is computed
* [docs/test-report.md](docs/test-report.md) final test report
* [PROGRESS.md](PROGRESS.md) build progress by step

## Project layout

```
backend/    Spring Boot (com.edusphere: auth, user, subject, content, storage, progress, quiz, assignment,
            community, notification, analytics, search, security, config, common, domain, repository)
frontend/   Vite app, feature-based folders under src/features
docs/  postman/  README.md  PROGRESS.md
```

## Deployment notes

* Build: `mvn -DskipTests package` produces `backend/target/edusphere-backend-1.0.0.jar`; `npm run build` produces `frontend/dist` (static files).
* Serve the frontend and the API under one HTTPS site if possible (for example a reverse proxy sending `/api` to port 8080) and set
  `COOKIE_SECURE=true`, `APP_FRONTEND_URL` to the public URL and `VITE_API_BASE_URL` to the public API URL before building the frontend.
* Use a dedicated MySQL user with rights on the one database only. 
* Put `STORAGE_PATH` on a persistent volume, outside the application folder, and back it up with the database.
* The login rate limiter is in memory: for several backend nodes move it to Redis or a gateway.
* Large videos: raise the proxy upload limit (the app accepts up to 500 MB per video; see `spring.servlet.multipart`).
