package com.edusphere.quiz;

import com.edusphere.quiz.QuizDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Quizzes")
public class QuizController {
    private final QuizService teacher;
    private final AttemptService student;

    public QuizController(QuizService teacher, AttemptService student) { this.teacher = teacher; this.student = student; }

    // ----- teacher -----
    @PostMapping("/teacher/subjects/{subjectId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz")
    public QuizSummary create(@PathVariable Long subjectId, @Valid @RequestBody QuizRequest req) { return teacher.create(subjectId, req); }

    @GetMapping("/teacher/subjects/{subjectId}/quizzes")
    @Operation(summary = "Quizzes of my subject")
    public List<QuizSummary> list(@PathVariable Long subjectId) { return teacher.list(subjectId); }

    @GetMapping("/teacher/quizzes/{id}")
    @Operation(summary = "Quiz with questions and correct answers")
    public TeacherQuiz get(@PathVariable Long id) { return teacher.get(id); }

    @PutMapping("/teacher/quizzes/{id}")
    @Operation(summary = "Update quiz settings")
    public QuizSummary update(@PathVariable Long id, @Valid @RequestBody QuizRequest req) { return teacher.update(id, req); }

    @DeleteMapping("/teacher/quizzes/{id}")
    @Operation(summary = "Delete a quiz")
    public Map<String, String> delete(@PathVariable Long id) {
        teacher.delete(id);
        return Map.of("message", "Quiz deleted");
    }

    @PostMapping("/teacher/quizzes/{id}/publish")
    @Operation(summary = "Publish or unpublish a quiz")
    public QuizSummary publish(@PathVariable Long id, @RequestBody PublishRequest req) { return teacher.setPublished(id, req.published()); }

    @PostMapping("/teacher/quizzes/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a question")
    public QuestionDto addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest req) { return teacher.addQuestion(id, req); }

    @PutMapping("/teacher/questions/{id}")
    @Operation(summary = "Edit a question")
    public QuestionDto updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest req) { return teacher.updateQuestion(id, req); }

    @DeleteMapping("/teacher/questions/{id}")
    @Operation(summary = "Delete a question")
    public Map<String, String> deleteQuestion(@PathVariable Long id) {
        teacher.deleteQuestion(id);
        return Map.of("message", "Question deleted");
    }

    @GetMapping("/teacher/quizzes/{id}/attempts")
    @Operation(summary = "All attempts of a quiz")
    public List<TeacherAttemptRow> attempts(@PathVariable Long id) { return teacher.attemptRows(id); }

    @GetMapping("/teacher/attempts/{id}")
    @Operation(summary = "Review one attempt")
    public AttemptResult attempt(@PathVariable Long id) { return teacher.teacherAttempt(id); }

    @PostMapping("/teacher/attempts/{id}/grade")
    @Operation(summary = "Grade short answers")
    public AttemptResult grade(@PathVariable Long id, @Valid @RequestBody GradeRequest req) { return teacher.grade(id, req); }

    @GetMapping("/teacher/quizzes/{id}/export.csv")
    @Operation(summary = "Export results as CSV")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] body = teacher.exportCsv(id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz-" + id + "-results.csv\"").body(body);
    }

    // ----- student -----
    @GetMapping("/student/subjects/{subjectId}/quizzes")
    @Operation(summary = "Quizzes available to me in a subject")
    public List<StudentQuiz> studentList(@PathVariable Long subjectId) { return student.listForSubject(subjectId); }

    @PostMapping("/student/quizzes/{id}/start")
    @Operation(summary = "Start (or resume) an attempt; the server sets the deadline")
    public AttemptView start(@PathVariable Long id) { return student.start(id); }

    @GetMapping("/student/attempts/{id}")
    @Operation(summary = "Resume an in-progress attempt or see its result")
    public Object attemptState(@PathVariable Long id) { return student.get(id); }

    @PutMapping("/student/attempts/{id}/answers")
    @Operation(summary = "Save answers while taking the quiz")
    public Map<String, Object> save(@PathVariable Long id, @Valid @RequestBody AnswersRequest req) { return student.saveAnswers(id, req.answers()); }

    @PostMapping("/student/attempts/{id}/submit")
    @Operation(summary = "Submit the attempt (only once)")
    public AttemptResult submit(@PathVariable Long id, @Valid @RequestBody(required = false) AnswersRequest req) {
        return student.submit(id, req == null ? null : req.answers());
    }
}
