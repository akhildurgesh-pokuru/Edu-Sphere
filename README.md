# EduSphere

EduSphere is a Udemy-style e-learning platform. **Teachers** build and publish courses. **Students** browse, enroll, learn, take quizzes, submit assignments and track their progress. Everything shown comes from real data entered by users; there is no sample content.

## Features

### For teachers
- Create, edit, publish and unpublish courses, with a cover image
- Build a curriculum of modules and lessons (video, PDF, document, link and text) with up/down reordering
- Upload files securely, with type, size and content checks
- **Quizzes:** multiple choice, true/false and short answer, with time limits, attempt limits and deadlines
  - Objective questions are graded automatically
  - Short answers are graded manually, with a grade history
  - Results can be exported to CSV
- **Assignments:** deadlines, file or text submissions, marks and feedback
- Announcements and discussion moderation
- **Analytics:** enrollments, completion, quiz and assignment performance, and students who are falling behind
- Invite other teachers by email link

### For students
- Course catalog with search, filters and pagination
- One-click enrollment, then a course player with progress tracking, resume-where-you-left-off and video seeking
- Take quizzes with a server-enforced timer, and view results with a review
- Submit assignments and see marks and feedback
- Discussion threads, announcements and a personal progress dashboard

### For everyone
- Email and password sign-up and login, plus **Sign in with Google** with an account chooser
- Forgot-password flow by email
- In-app notification bell, with an email copy of each notification that users can turn off on their profile
- Global search, dark mode, and a responsive, accessible interface (skip link, keyboard navigation, screen-reader announcements)

## Email
With SMTP configured, EduSphere sends:
- a welcome email after registration
- password reset and "password changed" emails
- teacher invitation emails
- an email copy of in-app notifications: new lessons, quizzes and assignments, grades, announcements, replies and deadline reminders

Emails are sent in the background after the change is saved, so a slow mail server never slows a request. Reset links are never written to the logs. A background job sends deadline reminders every hour.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS 3, Radix UI, React Router 6, TanStack Query 5, Axios, React Hook Form + Zod, Recharts, Framer Motion, Sonner, Lucide |
| Backend | Java 17, Spring Boot 3.5, Spring Security (session login, CSRF, Google OIDC), Spring Data JPA, Spring Mail, Flyway, springdoc OpenAPI |
| Database | MySQL 8 (`Edu-Sphere-database`) |
| Tests | JUnit 5, Mockito and MockMvc on the backend; Vitest and Testing Library on the frontend; Playwright for end-to-end |

## Project structure

```
Edu-Sphere/
├── backend/        Spring Boot API (auth, subjects, content, quizzes, assignments,
│                   community, notifications, analytics, search, mail, storage)
├── frontend/       React app (feature-based folders, shared UI components)
├── docs/           Architecture, ER diagram, security, analytics formulas, test report
├── postman/        Postman collection (101 requests)
└── README.md
```

## Security
- Passwords hashed with BCrypt (strength 12), with a login rate limiter (5 failures per 15 minutes per IP and email)
- HttpOnly, SameSite session cookie, session id rotated at login, and CSRF protection on every write request
- CORS restricted to the frontend origin, and security headers on every response
- Every protected endpoint checks the user's role and ownership. Other teachers' courses return 404, and students who are not enrolled cannot reach course content
- Password reset and invitation tokens are single-use, expire, and are stored only as SHA-256 hashes
- Uploads use an extension allow-list, file-signature checks, size limits, random file names, and are served only through an authorised download endpoint
- Google sign-in accepts verified emails only and always creates a student account
- Database changes are managed by versioned Flyway migrations, and Hibernate only validates the schema
- No stack traces or SQL in error responses, and spreadsheet-formula injection is blocked in CSV exports

## Getting started

### Prerequisites
Java 17, Maven, Node 18 or newer, and MySQL 8 running locally.

### 1. Configure the backend
Copy `backend/config/application.yml.example` to `backend/config/application.yml` (git-ignored) and set your MySQL password and, optionally, a demo teacher. The database is created automatically on first start.

### 2. Run
```bash
# Terminal 1: API on http://localhost:8080
cd backend && mvn spring-boot:run

# Terminal 2: app on http://localhost:5173
cd frontend && npm install && npm run dev
```
API docs: http://localhost:8080/swagger-ui.html

### 3. Try it
Sign in as the teacher you configured, create and publish a course, then register a student account and enroll.

## Configuration

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | local `Edu-Sphere-database`, root |
| `APP_FRONTEND_URL` | Used for CORS and for links in emails | `http://localhost:5173` |
| `STORAGE_PATH` | Where uploaded files are stored | `./uploads` |
| `ALLOW_TEACHER_SIGNUP` | Allow open teacher registration | `true` |
| `BOOTSTRAP_TEACHER_EMAIL`, `_PASSWORD`, `_NAME` | Creates a first teacher at startup | unset |
| `COOKIE_SECURE` | Set `true` when serving over HTTPS | `false` |
| `MAIL_ENABLED` | Turn on real emails | `false` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server | `localhost`, `587` |
| `MAIL_FROM`, `MAIL_FROM_NAME` | Sender address and name | `no-reply@edusphere.local`, `EduSphere` |
| `GOOGLE_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google sign-in | disabled |

### Gmail example
```bash
MAIL_ENABLED=true MAIL_HOST=smtp.gmail.com MAIL_PORT=587 \
MAIL_USERNAME=you@gmail.com MAIL_PASSWORD='your-app-password' MAIL_FROM=you@gmail.com
```
Use a Google **App Password** (requires 2-Step Verification), not your normal password.

### Google sign-in
1. In Google Cloud Console, create an OAuth client ID of type *Web application*.
2. Add the redirect URI `http://localhost:8080/login/oauth2/code/google`.
3. Start the backend with `GOOGLE_ENABLED=true`, `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` set.

Google always shows its account chooser, so the user picks which account to use.

## Testing
```bash
cd backend && mvn test            # unit and integration tests (separate Edu-Sphere-test database)
cd frontend && npm test           # component and unit tests
cd frontend && npm run build      # production build
cd frontend && npx playwright test   # end-to-end demo flow (needs both servers running)
```
Integration tests run on a separate database, so your real data is never touched. See `docs/test-report.md` for details and known limits.

## Documentation
- `docs/architecture.md`: how the system fits together
- `docs/er-diagram.md`: database tables and relationships
- `docs/security.md`: security design and known limits
- `docs/analytics-formulas.md`: how each dashboard number is calculated
- `docs/test-report.md`: test results
- `postman/EduSphere.postman_collection.json`: ready-made API requests
- Live API reference at `/swagger-ui.html`

## Known limits
- Join codes and approval-based enrollment are not available (enrollment is open)
- The login rate limiter is in memory, so it needs a shared store if you run several servers
- Uploaded files are not virus scanned
- Real video playback and a live Google or SMTP round trip depend on your own credentials and content
