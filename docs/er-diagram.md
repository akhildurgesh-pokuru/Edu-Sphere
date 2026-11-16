# EduSphere ER diagram

Source of truth: `backend/src/main/resources/db/migration/V1__init_schema.sql` (28 tables, owned by Flyway; Hibernate only validates).

```mermaid
erDiagram
    users ||--o{ user_roles : has
    roles ||--o{ user_roles : grants
    users ||--o{ oauth_accounts : links
    users ||--o{ password_reset_tokens : requests
    users ||--o{ teacher_invitations : creates

    users ||--o{ subjects : "teaches (teacher_id)"
    subjects ||--o{ modules : contains
    modules ||--o{ lessons : contains
    subjects ||--o{ learning_resources : stores
    learning_resources |o--o{ lessons : "attached to"

    subjects ||--o{ enrollments : has
    users ||--o{ enrollments : "joins (student_id)"
    subjects ||--o{ enrollment_invitations : has
    subjects ||--o{ enrollment_requests : has
    subjects ||--o{ subject_join_codes : has

    lessons ||--o{ lesson_progress : tracked
    users ||--o{ lesson_progress : makes

    subjects ||--o{ quizzes : has
    quizzes ||--o{ quiz_questions : has
    quiz_questions ||--o{ quiz_options : has
    quizzes ||--o{ quiz_attempts : has
    users ||--o{ quiz_attempts : takes
    quiz_attempts ||--o{ quiz_answers : has
    quiz_questions ||--o{ quiz_answers : answered
    quiz_answers ||--o{ quiz_grade_history : "grading history"

    subjects ||--o{ assignments : has
    assignments ||--o{ assignment_submissions : receives
    users ||--o{ assignment_submissions : submits
    learning_resources |o--o{ assignment_submissions : file

    subjects ||--o{ discussions : has
    discussions ||--o{ discussion_replies : has
    discussions ||--o{ discussion_reads : "read state"
    subjects ||--o{ announcements : has
    users ||--o{ notifications : receives
```

## Key constraints (enforced by the database, not only the code)

| Rule | Constraint |
|---|---|
| One enrollment per student and subject | `UNIQUE (subject_id, student_id)` on `enrollments` |
| One quiz attempt number per student | `UNIQUE (quiz_id, student_id, attempt_no)` on `quiz_attempts` |
| One answer per question per attempt | `UNIQUE (attempt_id, question_id)` on `quiz_answers` |
| One submission per assignment and student | `UNIQUE (assignment_id, student_id)` |
| One progress row per lesson and student | `UNIQUE (lesson_id, student_id)` |
| One notification per item (reminders) | `UNIQUE (user_id, ref_key)` on `notifications` |
| Single-use tokens are never stored raw | `token_hash` = SHA-256 hex, `UNIQUE` |
| Cascading cleanup | every child table has `ON DELETE CASCADE` (or `SET NULL` for optional file links) |
