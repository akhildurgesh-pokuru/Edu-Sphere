# Analytics formulas

Every number on a dashboard is computed by the backend from stored activity (`AnalyticsService`). Nothing is hard-coded.

| Metric | Formula |
|---|---|
| Subject progress (student) | completed **published** lessons / total **published** lessons (0 when there are none), rounded to 0.1 |
| Overall progress (student) | sum of completed published lessons / sum of published lessons over all enrolled, non-draft subjects |
| Quiz percent | `score / max_score * 100` of **GRADED** attempts (attempts waiting for manual grading are excluded) |
| Student quiz score for a quiz | best GRADED attempt |
| Average quiz score (teacher) | mean of quiz percent over all GRADED attempts in the teacher's subjects |
| Pass rate | GRADED attempts with percent >= passing percentage / GRADED attempts |
| Completion rate (subject) | students at 100% progress / ACTIVE enrolled students |
| Active student | any lesson progress update, quiz attempt start or assignment submission in the last 7 days |
| Needs support | quiz average < 50%, **or** inactive 14+ days with progress < 100%, **or** progress 0% a week after enrolling |
| Assignment completion | submissions / ACTIVE enrolled students; average marks percent = `marks / max_marks * 100` over graded submissions |
| Pending assignments | submissions with no marks yet |
| Enrollments over time | ACTIVE+LEFT+REMOVED enrollment rows grouped by UTC day for the last 30 days, zero-filled |
| Upcoming deadlines | assignments with a future due date, quizzes with a future close date (teacher: own subjects; student: not yet submitted / attempts left) |
