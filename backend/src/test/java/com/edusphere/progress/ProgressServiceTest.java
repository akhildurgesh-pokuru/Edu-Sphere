package com.edusphere.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.edusphere.domain.Lesson;
import com.edusphere.domain.LessonProgress;
import com.edusphere.repository.LessonProgressRepository;
import com.edusphere.repository.LessonRepository;
import com.edusphere.subject.AccessService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressServiceTest {
    @Mock LessonRepository lessons;
    @Mock LessonProgressRepository progress;
    @Mock AccessService access;
    @Mock NamedParameterJdbcTemplate jdbc;
    ProgressService service;

    private Lesson lesson(long id, boolean published, int duration) {
        Lesson l = new Lesson();
        l.setId(id); l.setSubjectId(9L); l.setModuleId(1L); l.setPublished(published); l.setDurationSeconds(duration);
        return l;
    }

    private LessonProgress done(long lessonId) {
        LessonProgress p = new LessonProgress();
        p.setLessonId(lessonId); p.setCompleted(true);
        return p;
    }

    @BeforeEach
    void setUp() {
        service = new ProgressService(lessons, progress, access, jdbc);
        when(progress.save(any(LessonProgress.class))).thenAnswer(i -> i.getArgument(0));
        when(progress.findByLessonIdAndStudentId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void subjectProgressIsCompletedOverPublishedLessons() {
        when(lessons.findBySubjectIdAndPublishedTrue(9L)).thenReturn(List.of(lesson(1, true, 0), lesson(2, true, 0), lesson(3, true, 0), lesson(4, true, 0)));
        when(progress.findBySubjectIdAndStudentId(9L, 7L)).thenReturn(List.of(done(1), done(2), done(3)));
        var p = service.forSubject(9L, 7L);
        assertThat(p.totalLessons()).isEqualTo(4);
        assertThat(p.completedLessons()).isEqualTo(3);
        assertThat(p.percent()).isEqualTo(75.0);
    }

    @Test
    void progressIsZeroForASubjectWithoutLessons() {
        when(lessons.findBySubjectIdAndPublishedTrue(9L)).thenReturn(List.of());
        assertThat(service.forSubject(9L, 7L).percent()).isZero();
    }

    @Test
    void unpublishedLessonsDoNotCountEvenIfCompleted() {
        when(lessons.findBySubjectIdAndPublishedTrue(9L)).thenReturn(List.of(lesson(1, true, 0), lesson(2, true, 0)));
        when(progress.findBySubjectIdAndStudentId(9L, 7L)).thenReturn(List.of(done(1), done(99)));
        assertThat(service.forSubject(9L, 7L).percent()).isEqualTo(50.0);
    }

    @Test
    void positionIsClampedToTheLessonDuration() {
        when(lessons.findById(1L)).thenReturn(Optional.of(lesson(1, true, 120)));
        assertThat(service.savePosition(1L, 7L, 5000).lastPositionSeconds()).isEqualTo(120);
        assertThat(service.savePosition(1L, 7L, -10).lastPositionSeconds()).isZero();
    }

    @Test
    void positionWithoutKnownDurationIsCappedAtTwelveHours() {
        when(lessons.findById(2L)).thenReturn(Optional.of(lesson(2, true, 0)));
        assertThat(service.savePosition(2L, 7L, Integer.MAX_VALUE).lastPositionSeconds()).isEqualTo(12 * 3600);
    }

    @Test
    void furthestPositionNeverGoesBackwards() {
        when(lessons.findById(1L)).thenReturn(Optional.of(lesson(1, true, 600)));
        LessonProgress existing = new LessonProgress();
        existing.setLessonId(1L); existing.setFurthestPositionSeconds(300);
        when(progress.findByLessonIdAndStudentId(1L, 7L)).thenReturn(Optional.of(existing));
        var r = service.savePosition(1L, 7L, 50);
        assertThat(r.lastPositionSeconds()).isEqualTo(50);
        assertThat(r.furthestPositionSeconds()).isEqualTo(300);
    }

    @Test
    void draftLessonsCannotBeTracked() {
        when(lessons.findById(3L)).thenReturn(Optional.of(lesson(3, false, 0)));
        org.junit.jupiter.api.Assertions.assertThrows(com.edusphere.common.ApiException.class, () -> service.setCompleted(3L, 7L, true));
    }

    @Test
    void completingStampsTheTimeAndUncompletingClearsIt() {
        when(lessons.findById(1L)).thenReturn(Optional.of(lesson(1, true, 0)));
        assertThat(service.setCompleted(1L, 7L, true).completed()).isTrue();
        assertThat(service.setCompleted(1L, 7L, false).completed()).isFalse();
    }
}
