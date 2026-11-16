package com.edusphere.quiz;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.quiz.QuizDtos.*;
import com.edusphere.repository.*;
import com.edusphere.subject.AccessService;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student side. Time limits and attempt limits are enforced here, on the server; correct answers are never sent early. */
@Service
public class AttemptService {
    static final long GRACE_SECONDS = 5;

    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final QuizOptionRepository options;
    private final QuizAttemptRepository attempts;
    private final QuizAnswerRepository answers;
    private final SubjectRepository subjects;
    private final AccessService access;
    private final NotificationService notifications;

    public AttemptService(QuizRepository quizzes, QuizQuestionRepository questions, QuizOptionRepository options,
                          QuizAttemptRepository attempts, QuizAnswerRepository answers, SubjectRepository subjects,
                          AccessService access, NotificationService notifications) {
        this.quizzes = quizzes; this.questions = questions; this.options = options; this.attempts = attempts;
        this.answers = answers; this.subjects = subjects; this.access = access; this.notifications = notifications;
    }

    @Transactional
    public List<StudentQuiz> listForSubject(Long subjectId) {
        access.requireEnrolledStudent(subjectId);
        Long sid = CurrentUser.id();
        List<Quiz> qs = quizzes.findBySubjectIdAndPublishedTrueOrderByCreatedAtDesc(subjectId);
        if (qs.isEmpty()) return List.of();
        // close attempts whose time ran out so lists never show stale "in progress" rows
        List<QuizAttempt> mine = attempts.findByStudentIdAndQuizIdIn(sid, qs.stream().map(Quiz::getId).toList());
        for (QuizAttempt a : mine) {
            if ("IN_PROGRESS".equals(a.getStatus()) && Instant.now().isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS))) {
                finalizeAttempt(attempts.lockById(a.getId()).orElse(a), true);
            }
        }
        Map<Long, List<QuizAttempt>> byQuiz = mine.stream().collect(Collectors.groupingBy(QuizAttempt::getQuizId));
        List<StudentQuiz> out = new ArrayList<>();
        Instant now = Instant.now();
        for (Quiz q : qs) {
            List<QuizQuestion> qq = questions.findByQuizIdOrderByPositionAscIdAsc(q.getId());
            List<QuizAttempt> as = byQuiz.getOrDefault(q.getId(), List.of()).stream().sorted(Comparator.comparingInt(QuizAttempt::getAttemptNo)).toList();
            Long inProgress = as.stream().filter(x -> "IN_PROGRESS".equals(x.getStatus())).map(QuizAttempt::getId).findFirst().orElse(null);
            String availability;
            if (inProgress != null) availability = "OPEN";
            else if (q.getStartAt() != null && now.isBefore(q.getStartAt())) availability = "UPCOMING";
            else if (q.getEndAt() != null && now.isAfter(q.getEndAt())) availability = "CLOSED";
            else if (as.size() >= q.getMaxAttempts()) availability = "NO_ATTEMPTS";
            else availability = "OPEN";
            Double best = as.stream().filter(x -> !"IN_PROGRESS".equals(x.getStatus()) && !x.isNeedsGrading()).map(QuizService::percent).max(Double::compare).orElse(null);
            out.add(new StudentQuiz(q.getId(), q.getTitle(), q.getDescription(), q.getDurationMinutes(), q.getMaxAttempts(), q.getPassingPercentage(),
                    q.getStartAt(), q.getEndAt(), qq.size(), qq.stream().mapToDouble(QuizQuestion::getMarks).sum(), availability, as.size(), inProgress, best,
                    as.stream().map(x -> new AttemptBrief(x.getId(), x.getAttemptNo(), x.getStatus(), x.getScore(), x.getMaxScore(),
                            QuizService.percent(x), x.getSubmittedAt())).toList()));
        }
        return out;
    }

    @Transactional
    public AttemptView start(Long quizId) {
        Long sid = CurrentUser.id();
        Quiz q = quizzes.findById(quizId).filter(Quiz::isPublished).orElseThrow(() -> ApiException.notFound("Quiz not found"));
        access.requireEnrolledStudent(q.getSubjectId());
        Instant now = Instant.now();
        Optional<QuizAttempt> open = attempts.findFirstByQuizIdAndStudentIdAndStatus(quizId, sid, "IN_PROGRESS");
        if (open.isPresent()) {
            QuizAttempt a = attempts.lockById(open.get().getId()).orElseThrow();
            if ("IN_PROGRESS".equals(a.getStatus())) {
                if (now.isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS))) finalizeAttempt(a, true);
                else return view(a, q);
            }
        }
        if (q.getStartAt() != null && now.isBefore(q.getStartAt())) throw ApiException.conflict("This quiz has not opened yet");
        if (q.getEndAt() != null && now.isAfter(q.getEndAt())) throw ApiException.conflict("This quiz is closed");
        long used = attempts.countByQuizIdAndStudentId(quizId, sid);
        if (used >= q.getMaxAttempts()) throw ApiException.conflict("You have used all attempts for this quiz");
        if (questions.findByQuizIdOrderByPositionAscIdAsc(quizId).isEmpty()) throw ApiException.conflict("This quiz has no questions yet");
        QuizAttempt a = new QuizAttempt();
        a.setQuizId(quizId);
        a.setStudentId(sid);
        a.setAttemptNo((int) used + 1);
        a.setStartedAt(now);
        Instant deadline = now.plus(Duration.ofMinutes(q.getDurationMinutes()));
        if (q.getEndAt() != null && q.getEndAt().isBefore(deadline)) deadline = q.getEndAt();
        a.setDeadlineAt(deadline); // computed on the server, never by the client
        a = attempts.save(a);
        return view(a, q);
    }

    private AttemptView view(QuizAttempt a, Quiz q) {
        List<QuizQuestion> qs = new ArrayList<>(questions.findByQuizIdOrderByPositionAscIdAsc(q.getId()));
        if (q.isRandomize()) Collections.shuffle(qs, new Random(a.getId()));
        Map<Long, List<QuizOption>> opts = options.findByQuestionIdInOrderByPositionAscIdAsc(qs.stream().map(QuizQuestion::getId).toList())
                .stream().collect(Collectors.groupingBy(QuizOption::getQuestionId));
        Map<Long, QuizAnswer> saved = answers.findByAttemptId(a.getId()).stream().collect(Collectors.toMap(QuizAnswer::getQuestionId, x -> x, (x, y) -> x));
        List<AttemptQuestion> out = qs.stream().map(qq -> {
            QuizAnswer s = saved.get(qq.getId());
            List<QuizOption> os = new ArrayList<>(opts.getOrDefault(qq.getId(), List.of()));
            if (q.isRandomize() && !"TRUE_FALSE".equals(qq.getType())) Collections.shuffle(os, new Random(a.getId() * 31 + qq.getId()));
            return new AttemptQuestion(qq.getId(), qq.getType(), qq.getText(), qq.getMarks(),
                    os.stream().map(o -> new OptionDto(o.getId(), o.getText(), null)).toList(),
                    s == null ? List.of() : AttemptMapper.parseIds(s.getSelectedOptionIds()), s == null ? null : s.getTextAnswer());
        }).toList();
        return new AttemptView(a.getId(), q.getId(), q.getTitle(), a.getAttemptNo(), a.getStartedAt(), a.getDeadlineAt(), Instant.now(), out);
    }

    private QuizAttempt ownAttempt(Long attemptId, boolean lock) {
        QuizAttempt a = (lock ? attempts.lockById(attemptId) : attempts.findById(attemptId)).orElseThrow(() -> ApiException.notFound("Attempt not found"));
        if (!a.getStudentId().equals(CurrentUser.id())) throw ApiException.notFound("Attempt not found");
        return a;
    }

    /** Resume an in-progress attempt, or show the result of a finished one. Returns either view or result. */
    @Transactional
    public Object get(Long attemptId) {
        QuizAttempt a = ownAttempt(attemptId, true);
        Quiz q = quizzes.findById(a.getQuizId()).orElseThrow();
        access.requireEnrolledStudent(q.getSubjectId());
        if ("IN_PROGRESS".equals(a.getStatus())) {
            if (Instant.now().isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS))) {
                finalizeAttempt(a, true);
            } else {
                return view(a, q);
            }
        }
        return AttemptMapper.result(a, q, questions, options, answers, false);
    }

    @Transactional
    public Map<String, Object> saveAnswers(Long attemptId, List<AnswerInput> input) {
        QuizAttempt a = ownAttempt(attemptId, true);
        Quiz q = quizzes.findById(a.getQuizId()).orElseThrow();
        access.requireEnrolledStudent(q.getSubjectId());
        if (!"IN_PROGRESS".equals(a.getStatus())) throw ApiException.conflict("This attempt is already submitted");
        if (Instant.now().isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS))) {
            finalizeAttempt(a, true);
            throw ApiException.conflict("Time is up. Your saved answers were submitted.");
        }
        store(a, q, input);
        return Map.of("saved", true, "deadlineAt", a.getDeadlineAt(), "serverNow", Instant.now());
    }

    @Transactional
    public AttemptResult submit(Long attemptId, List<AnswerInput> input) {
        QuizAttempt a = ownAttempt(attemptId, true);
        Quiz q = quizzes.findById(a.getQuizId()).orElseThrow();
        access.requireEnrolledStudent(q.getSubjectId());
        if (!"IN_PROGRESS".equals(a.getStatus())) throw ApiException.conflict("This attempt was already submitted");
        boolean late = Instant.now().isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS));
        if (!late && input != null) store(a, q, input); // after the deadline only already-saved answers count
        finalizeAttempt(a, late);
        return AttemptMapper.result(a, q, questions, options, answers, false);
    }

    private void store(QuizAttempt a, Quiz q, List<AnswerInput> input) {
        if (input == null) return;
        Map<Long, QuizQuestion> qById = questions.findByQuizIdOrderByPositionAscIdAsc(q.getId()).stream().collect(Collectors.toMap(QuizQuestion::getId, x -> x));
        Map<Long, Set<Long>> validOpts = options.findByQuestionIdInOrderByPositionAscIdAsc(qById.keySet()).stream()
                .collect(Collectors.groupingBy(QuizOption::getQuestionId, Collectors.mapping(QuizOption::getId, Collectors.toSet())));
        for (AnswerInput in : input) {
            QuizQuestion qq = qById.get(in.questionId());
            if (qq == null) throw ApiException.badRequest("Unknown question in answers");
            QuizAnswer ans = answers.findByAttemptIdAndQuestionId(a.getId(), qq.getId()).orElseGet(() -> {
                QuizAnswer n = new QuizAnswer();
                n.setAttemptId(a.getId());
                n.setQuestionId(qq.getId());
                return n;
            });
            if ("SHORT_ANSWER".equals(qq.getType())) {
                ans.setTextAnswer(in.text() == null || in.text().isBlank() ? null : in.text().trim());
                ans.setSelectedOptionIds(null);
            } else {
                List<Long> ids = in.optionIds() == null ? List.of() : in.optionIds().stream().distinct().sorted().toList();
                Set<Long> valid = validOpts.getOrDefault(qq.getId(), Set.of());
                if (!valid.containsAll(ids)) throw ApiException.badRequest("Invalid option for a question");
                if (!"MULTIPLE_SELECT".equals(qq.getType()) && ids.size() > 1) throw ApiException.badRequest("Only one option can be chosen");
                ans.setSelectedOptionIds(ids.isEmpty() ? null : ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
                ans.setTextAnswer(null);
            }
            answers.save(ans);
        }
    }

    /** Auto-grades objective questions; short answers wait for the teacher. */
    void finalizeAttempt(QuizAttempt a, boolean expired) {
        if (!"IN_PROGRESS".equals(a.getStatus())) return;
        Quiz q = quizzes.findById(a.getQuizId()).orElseThrow();
        List<QuizQuestion> qs = questions.findByQuizIdOrderByPositionAscIdAsc(q.getId());
        Map<Long, Set<Long>> correct = options.findByQuestionIdInOrderByPositionAscIdAsc(qs.stream().map(QuizQuestion::getId).toList()).stream()
                .filter(QuizOption::isCorrect).collect(Collectors.groupingBy(QuizOption::getQuestionId, Collectors.mapping(QuizOption::getId, Collectors.toSet())));
        Map<Long, QuizAnswer> saved = answers.findByAttemptId(a.getId()).stream().collect(Collectors.toMap(QuizAnswer::getQuestionId, x -> x, (x, y) -> x));
        double score = 0, max = 0;
        boolean needs = false;
        for (QuizQuestion qq : qs) {
            max += qq.getMarks();
            QuizAnswer ans = saved.get(qq.getId());
            if (ans == null) {
                ans = new QuizAnswer();
                ans.setAttemptId(a.getId());
                ans.setQuestionId(qq.getId());
            }
            if ("SHORT_ANSWER".equals(qq.getType())) {
                if (ans.getTextAnswer() == null) {
                    ans.setMarksAwarded(0.0);
                } else {
                    ans.setMarksAwarded(null);
                    needs = true;
                }
            } else {
                Set<Long> chosen = new HashSet<>(AttemptMapper.parseIds(ans.getSelectedOptionIds()));
                boolean ok = !chosen.isEmpty() && chosen.equals(correct.getOrDefault(qq.getId(), Set.of()));
                ans.setMarksAwarded(ok ? qq.getMarks() : 0.0);
                if (ok) score += qq.getMarks();
            }
            answers.save(ans);
        }
        a.setScore(score);
        a.setMaxScore(max);
        a.setNeedsGrading(needs);
        a.setExpired(expired);
        a.setSubmittedAt(Instant.now());
        a.setStatus(needs ? "PENDING_GRADING" : "GRADED");
        attempts.save(a);
        Subject s = subjects.findById(q.getSubjectId()).orElse(null);
        if (!needs) {
            notifications.notify(a.getStudentId(), "QUIZ_RESULT", "Quiz result: " + q.getTitle(), "You scored " + QuizService.percent(a) + "%",
                    "/student/attempts/" + a.getId(), "quizresult-" + a.getId());
        } else if (s != null) {
            notifications.notify(s.getTeacherId(), "QUIZ_RESULT", "Short answers to grade", q.getTitle(), "/teacher/quizzes/" + q.getId() + "/attempts", "grade-" + a.getId());
        }
    }
}
