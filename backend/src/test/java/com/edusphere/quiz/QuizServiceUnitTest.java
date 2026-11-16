package com.edusphere.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusphere.domain.QuizAttempt;
import org.junit.jupiter.api.Test;

class QuizServiceUnitTest {
    @Test
    void percentIsScoreOverMaxAndSafeForZeroMax() {
        QuizAttempt a = new QuizAttempt();
        a.setScore(7.5); a.setMaxScore(8.0);
        assertThat(QuizService.percent(a)).isEqualTo(93.8);
        a.setMaxScore(0.0);
        assertThat(QuizService.percent(a)).isZero();
    }

    @Test
    void csvCellsAreQuotedAndFormulaPrefixesNeutralised() {
        assertThat(QuizService.csv("Ada")).isEqualTo("\"Ada\"");
        assertThat(QuizService.csv("He said \"hi\"")).isEqualTo("\"He said \"\"hi\"\"\"");
        for (String evil : new String[]{"=1+1", "+cmd", "-2", "@SUM(A1)"}) assertThat(QuizService.csv(evil)).startsWith("\"'");
        assertThat(QuizService.csv(null)).isEqualTo("\"\"");
    }

    @Test
    void answerIdsAreParsedSafely() {
        assertThat(AttemptMapper.parseIds("1, 2,3")).containsExactly(1L, 2L, 3L);
        assertThat(AttemptMapper.parseIds(null)).isEmpty();
        assertThat(AttemptMapper.parseIds(" ")).isEmpty();
    }
}
