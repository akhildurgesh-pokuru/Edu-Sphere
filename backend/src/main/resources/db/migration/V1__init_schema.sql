-- EduSphere schema (owned by Flyway; Hibernate only validates).
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(190) NOT NULL,
  name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(100) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(30) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (name) VALUES ('TEACHER'), ('STUDENT');

CREATE TABLE oauth_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(30) NOT NULL,
  provider_subject VARCHAR(190) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_oauth_accounts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_oauth_accounts_provider_provider_subject (provider, provider_subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teacher_invitations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(190) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_teacher_invitations_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_teacher_invitations_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_password_reset_tokens_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
  id BIGINT NOT NULL AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  category VARCHAR(80),
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  enrollment_type VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  cover_image_path VARCHAR(255),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  published_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_subjects_teacher_id FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY ix_subjects_teacher_id (teacher_id),
  KEY ix_subjects_status_category (status, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE modules (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  position INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_modules_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  KEY ix_modules_subject_id_position (subject_id, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE learning_resources (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(100) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  kind VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_learning_resources_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_learning_resources_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_learning_resources_stored_name (stored_name),
  KEY ix_learning_resources_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lessons (
  id BIGINT NOT NULL AUTO_INCREMENT,
  module_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  type VARCHAR(20) NOT NULL,
  content_text TEXT,
  external_url VARCHAR(500),
  resource_id BIGINT,
  duration_seconds INT NOT NULL DEFAULT 0,
  position INT NOT NULL DEFAULT 0,
  published TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_lessons_module_id FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
  CONSTRAINT fk_lessons_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_lessons_resource_id FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE SET NULL,
  KEY ix_lessons_module_id_position (module_id, position),
  KEY ix_lessons_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE enrollments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  enrolled_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_enrollments_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollments_student_id FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_enrollments_subject_id_student_id (subject_id, student_id),
  KEY ix_enrollments_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE enrollment_invitations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  email VARCHAR(190) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_enrollment_invitations_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  UNIQUE KEY uq_enrollment_invitations_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE enrollment_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_enrollment_requests_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollment_requests_student_id FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_enrollment_requests_subject_id_student_id (subject_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subject_join_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  code VARCHAR(20) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  expires_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_subject_join_codes_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  UNIQUE KEY uq_subject_join_codes_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lesson_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,
  lesson_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  last_position_seconds INT NOT NULL DEFAULT 0,
  furthest_position_seconds INT NOT NULL DEFAULT 0,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  completed_at DATETIME(6),
  notes TEXT,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_lesson_progress_lesson_id FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  CONSTRAINT fk_lesson_progress_student_id FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_lesson_progress_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  UNIQUE KEY uq_lesson_progress_lesson_id_student_id (lesson_id, student_id),
  KEY ix_lesson_progress_subject_id_student_id (subject_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quizzes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  module_id BIGINT,
  lesson_id BIGINT,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  duration_minutes INT NOT NULL DEFAULT 30,
  max_attempts INT NOT NULL DEFAULT 1,
  passing_percentage INT NOT NULL DEFAULT 50,
  start_at DATETIME(6),
  end_at DATETIME(6),
  show_results TINYINT(1) NOT NULL DEFAULT 1,
  randomize TINYINT(1) NOT NULL DEFAULT 0,
  published TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_quizzes_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_quizzes_module_id FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE SET NULL,
  CONSTRAINT fk_quizzes_lesson_id FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE SET NULL,
  KEY ix_quizzes_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_questions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  quiz_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  text TEXT NOT NULL,
  marks DOUBLE NOT NULL DEFAULT 1,
  explanation TEXT,
  position INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_quiz_questions_quiz_id FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
  KEY ix_quiz_questions_quiz_id_position (quiz_id, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_options (
  id BIGINT NOT NULL AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  text VARCHAR(500) NOT NULL,
  correct TINYINT(1) NOT NULL DEFAULT 0,
  position INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_quiz_options_question_id FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
  KEY ix_quiz_options_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_attempts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  quiz_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  attempt_no INT NOT NULL,
  status VARCHAR(25) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at DATETIME(6) NOT NULL,
  deadline_at DATETIME(6) NOT NULL,
  submitted_at DATETIME(6),
  score DOUBLE NOT NULL DEFAULT 0,
  max_score DOUBLE NOT NULL DEFAULT 0,
  needs_grading TINYINT(1) NOT NULL DEFAULT 0,
  expired TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_quiz_attempts_quiz_id FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
  CONSTRAINT fk_quiz_attempts_student_id FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_quiz_attempts_quiz_id_student_id_attempt_no (quiz_id, student_id, attempt_no),
  KEY ix_quiz_attempts_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_answers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option_ids VARCHAR(500),
  text_answer TEXT,
  marks_awarded DOUBLE,
  feedback TEXT,
  graded_by BIGINT,
  graded_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_quiz_answers_attempt_id FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_quiz_answers_question_id FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
  CONSTRAINT fk_quiz_answers_graded_by FOREIGN KEY (graded_by) REFERENCES users(id) ON DELETE SET NULL,
  UNIQUE KEY uq_quiz_answers_attempt_id_question_id (attempt_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_grade_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  answer_id BIGINT NOT NULL,
  old_marks DOUBLE,
  new_marks DOUBLE NOT NULL,
  feedback TEXT,
  graded_by BIGINT NOT NULL,
  graded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_quiz_grade_history_answer_id FOREIGN KEY (answer_id) REFERENCES quiz_answers(id) ON DELETE CASCADE,
  CONSTRAINT fk_quiz_grade_history_graded_by FOREIGN KEY (graded_by) REFERENCES users(id) ON DELETE CASCADE,
  KEY ix_quiz_grade_history_answer_id (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE assignments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  instructions TEXT,
  due_at DATETIME(6) NOT NULL,
  max_marks DOUBLE NOT NULL DEFAULT 100,
  allowed_types VARCHAR(200) NOT NULL DEFAULT 'pdf,doc,docx,ppt,pptx,txt',
  attachment_resource_id BIGINT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_assignments_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_assignments_attachment_resource_id FOREIGN KEY (attachment_resource_id) REFERENCES learning_resources(id) ON DELETE SET NULL,
  KEY ix_assignments_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE assignment_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  resource_id BIGINT,
  submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  late TINYINT(1) NOT NULL DEFAULT 0,
  marks DOUBLE,
  feedback TEXT,
  graded_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_assignment_submissions_assignment_id FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_submissions_student_id FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_submissions_resource_id FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE SET NULL,
  UNIQUE KEY uq_assignment_submissions_assignment_id_student_id (assignment_id, student_id),
  KEY ix_assignment_submissions_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE discussions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  pinned TINYINT(1) NOT NULL DEFAULT 0,
  closed TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_activity_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_discussions_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_discussions_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY ix_discussions_subject_id_last_activity_at (subject_id, last_activity_at),
  FULLTEXT KEY ft_discussions_text (title, body)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE discussion_replies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  discussion_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  body TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_discussion_replies_discussion_id FOREIGN KEY (discussion_id) REFERENCES discussions(id) ON DELETE CASCADE,
  CONSTRAINT fk_discussion_replies_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY ix_discussion_replies_discussion_id (discussion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE discussion_reads (
  id BIGINT NOT NULL AUTO_INCREMENT,
  discussion_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_read_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_discussion_reads_discussion_id FOREIGN KEY (discussion_id) REFERENCES discussions(id) ON DELETE CASCADE,
  CONSTRAINT fk_discussion_reads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_discussion_reads_discussion_id_user_id (discussion_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcements (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  pinned TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_announcements_subject_id FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_announcements_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY ix_announcements_subject_id_created_at (subject_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message VARCHAR(500),
  link VARCHAR(300),
  ref_key VARCHAR(120),
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_notifications_user_id_ref_key (user_id, ref_key),
  KEY ix_notifications_user_id_is_read_created_at (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
