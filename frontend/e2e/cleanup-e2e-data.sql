-- The Playwright demo flow registers timestamped accounts (*@e2e.local) in whichever database the backend uses.
-- Remove them (subjects, enrollments, quizzes, files metadata ... cascade through the foreign keys):
--   mysql -u root -p "Edu-Sphere-database" < frontend/e2e/cleanup-e2e-data.sql
DELETE FROM users WHERE email LIKE '%@e2e.local';
