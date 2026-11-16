package com.edusphere.quiz;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.quiz.QuizDtos.*;
import com.edusphere.repository.*;
import com.edusphere.subject.AccessService;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Teacher side: quiz builder, attempts, manual grading, CSV export. */
@Service
public class QuizService {
    static final Set<String> TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_SELECT", "TRUE_FALSE", "SHORT_ANSWER");

    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final QuizOptionRepository options;
    private final QuizAttemptRepository attempts;
    private final QuizAnswerRepository answers;
    private final QuizGradeHistoryRepository history;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;

    public QuizService(QuizRepository quizzes, QuizQuestionRepository questions, QuizOptionRepository options,
                       QuizAttemptRepository attempts, QuizAnswerRepository answers, QuizGradeHistoryRepository history,
                       CourseModuleRepository modules, LessonRepository lessons, EnrollmentRepository enrollments,
                       UserRepository users, AccessService access, NotificationService notifications) {
        this.quizzes = quizzes; this.questions = questions; this.options = options; this.attempts = attempts;
        this.answers = answers; this.history = history; this.modules = modules; this.lessons = lessons;
        this.enrollments = enrollments; this.users = users; this.access = access; this.notifications = notifications;
    }

    Quiz ownedQuiz(Long id) {
        Quiz q = quizzes.findById(id).orElseThrow(() -> ApiException.notFound("Quiz not found"));
        access.requireOwnedSubject(q.getSubjectId());
        return q;
    }

    private QuizSummary summary(Quiz q, List<QuizQuestion> qs, long attemptCount) {
        double total = qs.stream().mapToDouble(QuizQuestion::getMarks).sum();
        return new QuizSummary(q.getId(), q.getSubjectId(), q.getModuleId(), q.getLessonId(), q.getTitle(), q.getDescription(), q.isPublished(),
                q.getDurationMinutes(), q.getMaxAttempts(), q.getPassingPercentage(), q.getStartAt(), q.getEndAt(), q.isShowResults(),
                q.isRandomize(), qs.size(), total, attemptCount);
    }

    private void applyRequest(Quiz q, QuizRequest r) {
        if (r.startAt() != null && r.endAt() != null && !r.endAt().isAfter(r.startAt())) {
            throw ApiException.badRequest("The end date must be after the start date");
        }
        if (r.moduleId() != null && modules.findByIdAndSubjectId(r.moduleId(), q.getSubjectId()).isEmpty()) {
            throw ApiException.badRequest("Module does not belong to this subject");
        }
        if (r.lessonId() != null && lessons.findById(r.lessonId()).filter(l -> l.getSubjectId().equals(q.getSubjectId())).isEmpty()) {
            throw ApiException.badRequest("Lesson does not belong to this subject");
        }
        q.setTitle(r.title().trim());
        q.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        q.setModuleId(r.moduleId());
        q.setLessonId(r.lessonId());
        q.setDurationMinutes(r.durationMinutes());
        q.setMaxAttempts(r.maxAttempts());
        q.setPassingPercentage(r.passingPercentage());
        q.setStartAt(r.startAt());
        q.setEndAt(r.endAt());
        q.setShowResults(r.showResults() == null || r.showResults());
        q.setRandomize(Boolean.TRUE.equals(r.randomize()));
        q.setUpdatedAt(Instant.now());
    }

    @Transactional
    public QuizSummary create(Long subjectId, QuizRequest r) {
        access.requireOwnedSubject(subjectId);
        Quiz q = new Quiz();
        q.setSubjectId(subjectId);
        applyRequest(q, r);
        q = quizzes.save(q);
        return summary(q, List.of(), 0);
    }

    @Transactional
    public QuizSummary update(Long id, QuizRequest r) {
        Quiz q = ownedQuiz(id);
        applyRequest(q, r);
        quizzes.save(q);
        return summary(q, questions.findByQuizIdOrderByPositionAscIdAsc(q.getId()), attempts.findByQuizIdOrderByStartedAtDesc(id).size());
    }

    @Transactional
    public void delete(Long id) {
        quizzes.delete(ownedQuiz(id));
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> list(Long subjectId) {
        access.requireOwnedSubject(subjectId);
        return quizzes.findBySubjectIdOrderByCreatedAtDesc(subjectId).stream()
                .map(q -> summary(q, questions.findByQuizIdOrderByPositionAscIdAsc(q.getId()), attempts.findByQuizIdOrderByStartedAtDesc(q.getId()).size())).toList();
    }

    @Transactional(readOnly = true)
    public TeacherQuiz get(Long id) {
        Quiz q = ownedQuiz(id);
        List<QuizQuestion> qs = questions.findByQuizIdOrderByPositionAscIdAsc(id);
        return new TeacherQuiz(summary(q, qs, attempts.findByQuizIdOrderByStartedAtDesc(id).size()), toQuestionDtos(qs, true));
    }

    List<QuestionDto> toQuestionDtos(List<QuizQuestion> qs, boolean withCorrect) {
        Map<Long, List<QuizOption>> opts = qs.isEmpty() ? Map.of() : options.findByQuestionIdInOrderByPositionAscIdAsc(qs.stream().map(QuizQuestion::getId).toList())
                .stream().collect(Collectors.groupingBy(QuizOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
        return qs.stream().map(x -> new QuestionDto(x.getId(), x.getType(), x.getText(), x.getMarks(), x.getExplanation(), x.getPosition(),
                opts.getOrDefault(x.getId(), List.of()).stream().map(o -> new OptionDto(o.getId(), o.getText(), withCorrect ? o.isCorrect() : null)).toList())).toList();
    }

    @Transactional
    public QuizSummary setPublished(Long id, boolean published) {
        Quiz q = ownedQuiz(id);
        List<QuizQuestion> qs = questions.findByQuizIdOrderByPositionAscIdAsc(id);
        if (published && qs.isEmpty()) throw ApiException.badRequest("Add at least one question before publishing");
        boolean was = q.isPublished();
        q.setPublished(published);
        q.setUpdatedAt(Instant.now());
        quizzes.save(q);
        if (published && !was) {
            Subject unused = access.requireOwnedSubject(q.getSubjectId());
            for (Enrollment e : enrollments.findBySubjectIdAndStatus(q.getSubjectId(), "ACTIVE")) {
                notifications.notify(e.getStudentId(), "NEW_QUIZ", "New quiz: " + q.getTitle(), unused.getTitle(),
                        "/student/subjects/" + q.getSubjectId() + "?tab=quizzes", "quiz-" + q.getId());
            }
        }
        return summary(q, qs, attempts.findByQuizIdOrderByStartedAtDesc(id).size());
    }

    // ---------- questions ----------
    private void requireNoAttempts(Long quizId) {
        if (!attempts.findByQuizIdOrderByStartedAtDesc(quizId).isEmpty()) {
            throw ApiException.conflict("Questions cannot be changed after students have started this quiz");
        }
    }

    @Transactional
    public QuestionDto addQuestion(Long quizId, QuestionRequest r) {
        Quiz q = ownedQuiz(quizId);
        requireNoAttempts(quizId);
        QuizQuestion qq = new QuizQuestion();
        qq.setQuizId(q.getId());
        qq.setPosition(questions.findByQuizIdOrderByPositionAscIdAsc(quizId).size());
        saveQuestion(qq, r);
        return toQuestionDtos(List.of(qq), true).get(0);
    }

    @Transactional
    public QuestionDto updateQuestion(Long questionId, QuestionRequest r) {
        QuizQuestion qq = questions.findById(questionId).orElseThrow(() -> ApiException.notFound("Question not found"));
        ownedQuiz(qq.getQuizId());
        requireNoAttempts(qq.getQuizId());
        saveQuestion(qq, r);
        return toQuestionDtos(List.of(qq), true).get(0);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        QuizQuestion qq = questions.findById(questionId).orElseThrow(() -> ApiException.notFound("Question not found"));
        ownedQuiz(qq.getQuizId());
        requireNoAttempts(qq.getQuizId());
        questions.delete(qq);
    }

    private void saveQuestion(QuizQuestion qq, QuestionRequest r) {
        String type = r.type().trim().toUpperCase();
        if (!TYPES.contains(type)) throw ApiException.badRequest("Unknown question type");
        List<OptionRequest> opts = new ArrayList<>();
        switch (type) {
            case "TRUE_FALSE" -> {
                if (r.trueFalseAnswer() == null) throw ApiException.badRequest("Choose whether the statement is true or false");
                opts.add(new OptionRequest("True", r.trueFalseAnswer()));
                opts.add(new OptionRequest("False", !r.trueFalseAnswer()));
            }
            case "SHORT_ANSWER" -> { }
            default -> {
                opts.addAll(r.options() == null ? List.of() : r.options());
                long correct = opts.stream().filter(OptionRequest::correct).count();
                if (opts.size() < 2) throw ApiException.badRequest("Add at least two options");
                if (opts.size() > 10) throw ApiException.badRequest("At most 10 options");
                if ("SINGLE_CHOICE".equals(type) && correct != 1) throw ApiException.badRequest("Mark exactly one option as correct");
                if ("MULTIPLE_SELECT".equals(type) && correct < 1) throw ApiException.badRequest("Mark at least one option as correct");
            }
        }
        qq.setType(type);
        qq.setText(r.text().trim());
        qq.setMarks(r.marks());
        qq.setExplanation(r.explanation() == null || r.explanation().isBlank() ? null : r.explanation().trim());
        qq = questions.save(qq);
        if (qq.getId() != null) {
            List<QuizOption> old = options.findByQuestionIdInOrderByPositionAscIdAsc(List.of(qq.getId()));
            options.deleteAll(old);
            options.flush();
        }
        int pos = 0;
        for (OptionRequest o : opts) {
            QuizOption qo = new QuizOption();
            qo.setQuestionId(qq.getId());
            qo.setText(o.text().trim());
            qo.setCorrect(o.correct());
            qo.setPosition(pos++);
            options.save(qo);
        }
    }

    // ---------- attempts & grading ----------
    @Transactional(readOnly = true)
    public List<TeacherAttemptRow> attemptRows(Long quizId) {
        ownedQuiz(quizId);
        List<QuizAttempt> list = attempts.findByQuizIdOrderByStartedAtDesc(quizId);
        Map<Long, User> byId = users.findAllById(list.stream().map(QuizAttempt::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        return list.stream().map(a -> {
            User u = byId.get(a.getStudentId());
            return new TeacherAttemptRow(a.getId(), a.getStudentId(), u == null ? "Student" : u.getName(), u == null ? "" : u.getEmail(), a.getAttemptNo(),
                    a.getStatus(), a.getScore(), a.getMaxScore(), percent(a), a.isNeedsGrading(), a.getStartedAt(), a.getSubmittedAt());
        }).toList();
    }

    static double percent(QuizAttempt a) {
        return a.getMaxScore() <= 0 ? 0 : Math.round(a.getScore() * 1000.0 / a.getMaxScore()) / 10.0;
    }

    @Transactional(readOnly = true)
    public AttemptResult teacherAttempt(Long attemptId) {
        QuizAttempt a = attempts.findById(attemptId).orElseThrow(() -> ApiException.notFound("Attempt not found"));
        Quiz q = ownedQuiz(a.getQuizId());
        return AttemptMapper.result(a, q, questions, options, answers, true);
    }

    @Transactional
    public AttemptResult grade(Long attemptId, GradeRequest req) {
        QuizAttempt a = attempts.findById(attemptId).orElseThrow(() -> ApiException.notFound("Attempt not found"));
        Quiz q = ownedQuiz(a.getQuizId());
        if ("IN_PROGRESS".equals(a.getStatus())) throw ApiException.conflict("This attempt is still in progress");
        Map<Long, QuizAnswer> byId = answers.findByAttemptId(attemptId).stream().collect(Collectors.toMap(QuizAnswer::getId, x -> x));
        Map<Long, QuizQuestion> qById = questions.findByQuizIdOrderByPositionAscIdAsc(q.getId()).stream().collect(Collectors.toMap(QuizQuestion::getId, x -> x));
        for (GradeInput g : req.grades()) {
            QuizAnswer ans = byId.get(g.answerId());
            if (ans == null) throw ApiException.badRequest("Answer does not belong to this attempt");
            QuizQuestion qq = qById.get(ans.getQuestionId());
            if (!"SHORT_ANSWER".equals(qq.getType())) throw ApiException.badRequest("Only short answers are graded manually");
            if (g.marks() > qq.getMarks()) throw ApiException.badRequest("Marks cannot exceed " + qq.getMarks());
            QuizGradeHistory h = new QuizGradeHistory();
            h.setAnswerId(ans.getId());
            h.setOldMarks(ans.getMarksAwarded());
            h.setNewMarks(g.marks());
            h.setFeedback(g.feedback());
            h.setGradedBy(CurrentUser.id());
            history.save(h);
            ans.setMarksAwarded(g.marks());
            ans.setFeedback(g.feedback() == null || g.feedback().isBlank() ? null : g.feedback().trim());
            ans.setGradedBy(CurrentUser.id());
            ans.setGradedAt(Instant.now());
            answers.save(ans);
        }
        List<QuizAnswer> all = answers.findByAttemptId(attemptId);
        boolean stillPending = all.stream().anyMatch(x -> qById.get(x.getQuestionId()) != null && "SHORT_ANSWER".equals(qById.get(x.getQuestionId()).getType()) && x.getMarksAwarded() == null);
        a.setScore(all.stream().mapToDouble(x -> x.getMarksAwarded() == null ? 0 : x.getMarksAwarded()).sum());
        boolean wasPending = a.isNeedsGrading();
        a.setNeedsGrading(stillPending);
        a.setStatus(stillPending ? "PENDING_GRADING" : "GRADED");
        attempts.save(a);
        if (wasPending && !stillPending) {
            notifications.notify(a.getStudentId(), "QUIZ_RESULT", "Quiz graded: " + q.getTitle(),
                    "You scored " + percent(a) + "%", "/student/attempts/" + a.getId(), "quizresult-" + a.getId());
        }
        return AttemptMapper.result(a, q, questions, options, answers, true);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long quizId) {
        ownedQuiz(quizId);
        StringBuilder sb = new StringBuilder("Student,Email,Attempt,Status,Score,Max score,Percent,Started,Submitted\n");
        for (TeacherAttemptRow r : attemptRows(quizId)) {
            sb.append(csv(r.studentName())).append(',').append(csv(r.studentEmail())).append(',').append(r.attemptNo()).append(',')
              .append(r.status()).append(',').append(r.score()).append(',').append(r.maxScore()).append(',').append(r.percent()).append(',')
              .append(r.startedAt()).append(',').append(r.submittedAt() == null ? "" : r.submittedAt()).append('\n');
        }
        return sb.toString();
    }

    static String csv(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s; // prevent spreadsheet formula injection
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
