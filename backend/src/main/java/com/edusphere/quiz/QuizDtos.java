package com.edusphere.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class QuizDtos {
    private QuizDtos() {}

    public record QuizRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 5000, message = "Description is too long") String description,
            Long moduleId, Long lessonId,
            @NotNull(message = "Duration is required") @Min(value = 1, message = "At least 1 minute") @Max(value = 600, message = "At most 600 minutes") Integer durationMinutes,
            @NotNull(message = "Max attempts is required") @Min(value = 1, message = "At least 1 attempt") @Max(value = 20, message = "At most 20 attempts") Integer maxAttempts,
            @NotNull(message = "Passing percentage is required") @Min(value = 0, message = "0-100") @Max(value = 100, message = "0-100") Integer passingPercentage,
            Instant startAt, Instant endAt, Boolean showResults, Boolean randomize) {}

    public record PublishRequest(boolean published) {}

    public record OptionRequest(@NotBlank(message = "Option text is required") @Size(max = 500) String text, boolean correct) {}

    public record QuestionRequest(
            @NotBlank(message = "Question type is required") String type,
            @NotBlank(message = "Question text is required") @Size(max = 5000, message = "Question is too long") String text,
            @NotNull(message = "Marks are required") @DecimalMin(value = "0.5", message = "At least 0.5 marks") @DecimalMax(value = "1000", message = "Too many marks") Double marks,
            @Size(max = 5000) String explanation,
            @Valid List<OptionRequest> options,
            Boolean trueFalseAnswer) {}

    public record OptionDto(Long id, String text, Boolean correct) {}

    public record QuestionDto(Long id, String type, String text, double marks, String explanation, int position, List<OptionDto> options) {}

    public record QuizSummary(Long id, Long subjectId, Long moduleId, Long lessonId, String title, String description, boolean published,
                              int durationMinutes, int maxAttempts, int passingPercentage, Instant startAt, Instant endAt,
                              boolean showResults, boolean randomize, int questionCount, double totalMarks, long attemptCount) {}

    public record TeacherQuiz(QuizSummary quiz, List<QuestionDto> questions) {}

    // ----- student -----
    public record AttemptBrief(Long id, int attemptNo, String status, Double score, Double maxScore, Double percent, Instant submittedAt) {}

    public record StudentQuiz(Long id, String title, String description, int durationMinutes, int maxAttempts, int passingPercentage,
                              Instant startAt, Instant endAt, int questionCount, double totalMarks, String availability,
                              int attemptsUsed, Long inProgressAttemptId, Double bestPercent, List<AttemptBrief> attempts) {}

    public record AttemptQuestion(Long id, String type, String text, double marks, List<OptionDto> options, List<Long> selectedOptionIds, String textAnswer) {}

    public record AttemptView(Long attemptId, Long quizId, String title, int attemptNo, Instant startedAt, Instant deadlineAt,
                              Instant serverNow, List<AttemptQuestion> questions) {}

    public record AnswerInput(@NotNull Long questionId, List<Long> optionIds, @Size(max = 10000) String text) {}

    public record AnswersRequest(@Valid List<AnswerInput> answers) {}

    public record ReviewItem(Long questionId, String type, String text, double marks, List<OptionDto> options, List<Long> selectedOptionIds,
                             List<Long> correctOptionIds, String textAnswer, Double marksAwarded, String explanation, String feedback, Long answerId) {}

    public record AttemptResult(Long attemptId, Long quizId, String quizTitle, int attemptNo, String status, boolean expired,
                                boolean pendingGrading, double score, double maxScore, double percent, boolean passed,
                                Instant submittedAt, boolean reviewAllowed, List<ReviewItem> review) {}

    // ----- teacher attempts -----
    public record TeacherAttemptRow(Long id, Long studentId, String studentName, String studentEmail, int attemptNo, String status,
                                    double score, double maxScore, double percent, boolean needsGrading, Instant startedAt, Instant submittedAt) {}

    public record GradeInput(@NotNull Long answerId, @NotNull @DecimalMin("0") Double marks, @Size(max = 5000) String feedback) {}

    public record GradeRequest(@NotEmpty(message = "Provide at least one grade") @Valid List<GradeInput> grades) {}
}
