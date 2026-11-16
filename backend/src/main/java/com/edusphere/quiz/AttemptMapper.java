package com.edusphere.quiz;

import com.edusphere.domain.*;
import com.edusphere.quiz.QuizDtos.*;
import com.edusphere.repository.QuizAnswerRepository;
import com.edusphere.repository.QuizOptionRepository;
import com.edusphere.repository.QuizQuestionRepository;
import java.util.*;
import java.util.stream.Collectors;

final class AttemptMapper {
    private AttemptMapper() {}

    static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(Long::valueOf).toList();
    }

    static AttemptResult result(QuizAttempt a, Quiz q, QuizQuestionRepository questions, QuizOptionRepository options,
                                QuizAnswerRepository answers, boolean teacherView) {
        double pct = QuizService.percent(a);
        boolean reviewAllowed = teacherView || q.isShowResults();
        List<ReviewItem> review = List.of();
        if (reviewAllowed) {
            List<QuizQuestion> qs = questions.findByQuizIdOrderByPositionAscIdAsc(q.getId());
            Map<Long, List<QuizOption>> opts = qs.isEmpty() ? Map.of() : options.findByQuestionIdInOrderByPositionAscIdAsc(qs.stream().map(QuizQuestion::getId).toList())
                    .stream().collect(Collectors.groupingBy(QuizOption::getQuestionId));
            Map<Long, QuizAnswer> byQ = answers.findByAttemptId(a.getId()).stream().collect(Collectors.toMap(QuizAnswer::getQuestionId, x -> x, (x, y) -> x));
            List<ReviewItem> items = new ArrayList<>();
            for (QuizQuestion qq : qs) {
                QuizAnswer ans = byQ.get(qq.getId());
                List<QuizOption> os = opts.getOrDefault(qq.getId(), List.of());
                items.add(new ReviewItem(qq.getId(), qq.getType(), qq.getText(), qq.getMarks(),
                        os.stream().map(o -> new OptionDto(o.getId(), o.getText(), null)).toList(),
                        ans == null ? List.of() : parseIds(ans.getSelectedOptionIds()),
                        os.stream().filter(QuizOption::isCorrect).map(QuizOption::getId).toList(),
                        ans == null ? null : ans.getTextAnswer(), ans == null ? null : ans.getMarksAwarded(),
                        qq.getExplanation(), ans == null ? null : ans.getFeedback(), ans == null ? null : ans.getId()));
            }
            review = items;
        }
        boolean pending = a.isNeedsGrading();
        return new AttemptResult(a.getId(), q.getId(), q.getTitle(), a.getAttemptNo(), a.getStatus(), a.isExpired(), pending, a.getScore(),
                a.getMaxScore(), pct, !pending && pct >= q.getPassingPercentage(), a.getSubmittedAt(), reviewAllowed, review);
    }
}
