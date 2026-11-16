package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FileAccessIntegrationTest extends IntegrationTestBase {

    private long uploadPdf(Sess t, long subjectId) throws Exception {
        return idOf(upload(t, "/api/teacher/subjects/" + subjectId + "/resources", "notes.pdf", "application/pdf", PDF).andExpect(status().isCreated()));
    }

    @Test
    void uploadsAreValidatedByContentNotJustExtension() throws Exception {
        Sess t = teacher();
        long s = createSubject(t, "Uploads");
        upload(t, "/api/teacher/subjects/" + s + "/resources", "evil.pdf", "application/pdf", "#!/bin/sh\nrm -rf /".getBytes()).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + s + "/resources", "run.sh", "text/plain", "echo hi".getBytes()).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + s + "/resources", "app.exe", "application/octet-stream", new byte[]{'M', 'Z', 0, 0}).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + s + "/resources", "fake.mp4", "video/mp4", "not a video at all".getBytes()).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + s + "/resources", "empty.pdf", "application/pdf", new byte[0]).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + s + "/resources", "ok.pdf", "application/pdf", PDF).andExpect(status().isCreated()).andExpect(jsonPath("$.kind").value("PDF"));
        upload(t, "/api/teacher/subjects/" + s + "/resources", "movie.mp4", "video/mp4", mp4()).andExpect(status().isCreated()).andExpect(jsonPath("$.kind").value("VIDEO"));
    }

    @Test
    void storedNamesAreRandomAndNeverTheUserFilename() throws Exception {
        Sess t = teacher();
        long s = createSubject(t, "Names");
        upload(t, "/api/teacher/subjects/" + s + "/resources", "../../etc/passwd.pdf", "application/pdf", PDF).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("passwd.pdf"));
        String stored = jdbc.queryForObject("select stored_name from learning_resources where subject_id = ? order by id desc limit 1", String.class, s);
        assertThat(stored).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.pdf").doesNotContain("passwd");
    }

    @Test
    void onlyOwnerAndEnrolledStudentsCanDownload() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long pdf = uploadPdf(t, ids[0]);
        post(t, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Slides", "type", "DOCUMENT", "resourceId", pdf, "published", true)).andExpect(status().isCreated());

        get(null, "/api/resources/" + pdf).andExpect(status().isUnauthorized());
        get(student(), "/api/resources/" + pdf).andExpect(status().isNotFound());   // not enrolled
        get(teacher(), "/api/resources/" + pdf).andExpect(status().isNotFound());   // other teacher
        get(t, "/api/resources/" + pdf).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff"));

        Sess s = student();
        enroll(s, ids[0]);
        get(s, "/api/resources/" + pdf).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        get(s, "/api/resources/99999999").andExpect(status().isNotFound());
    }

    @Test
    void filesOfDraftLessonsAndOrphanUploadsStayHiddenFromStudents() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long orphan = uploadPdf(t, ids[0]);
        long draftFile = uploadPdf(t, ids[0]);
        post(t, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Draft", "type", "DOCUMENT", "resourceId", draftFile, "published", false)).andExpect(status().isCreated());
        Sess s = student();
        enroll(s, ids[0]);
        get(s, "/api/resources/" + orphan).andExpect(status().isNotFound());
        get(s, "/api/resources/" + draftFile).andExpect(status().isNotFound());
        get(t, "/api/resources/" + draftFile).andExpect(status().isOk());
    }

    @Test
    void videosSupportHttpRangeRequests() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long vid = idOf(upload(t, "/api/teacher/subjects/" + ids[0] + "/resources", "movie.mp4", "video/mp4", mp4()).andExpect(status().isCreated()));
        post(t, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Video", "type", "VIDEO", "resourceId", vid, "durationSeconds", 60, "published", true)).andExpect(status().isCreated());
        Sess s = student();
        enroll(s, ids[0]);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/resources/" + vid).session(s.session()).header("Range", "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-99/2048"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().longValue("Content-Length", 100));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/resources/" + vid).session(s.session()).header("Range", "bytes=1000-")).andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 1000-2047/2048"));
    }

    @Test
    void lessonCannotUseAnotherSubjectsFileOrWrongKind() throws Exception {
        Sess a = teacher();
        long[] ids = publishedSubject(a);
        Sess b = teacher();
        long other = createSubject(b, "Other");
        long foreignFile = uploadPdf(b, other);
        post(a, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Steal", "type", "DOCUMENT", "resourceId", foreignFile)).andExpect(status().isBadRequest());
        long pdf = uploadPdf(a, ids[0]);
        post(a, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Wrong", "type", "VIDEO", "resourceId", pdf)).andExpect(status().isBadRequest());
        post(a, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Link", "type", "LINK", "externalUrl", "javascript:alert(1)")).andExpect(status().isBadRequest());
        post(a, "/api/teacher/modules/" + ids[1] + "/lessons", Map.of("title", "Link", "type", "LINK", "externalUrl", "https://example.com/x")).andExpect(status().isCreated());
        // students cannot upload
        post(student(), "/api/teacher/subjects/" + ids[0] + "/resources", null).andExpect(status().isForbidden());
    }

    @Test
    void coverImagesAreValidatedAndServedOnlyToThoseWhoMaySeeTheSubject() throws Exception {
        Sess t = teacher();
        long draft = createSubject(t, "Cover draft");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0, 0, 0, 0, 0};
        upload(t, "/api/teacher/subjects/" + draft + "/cover", "c.png", "image/png", "not an image".getBytes()).andExpect(status().isBadRequest());
        upload(t, "/api/teacher/subjects/" + draft + "/cover", "c.png", "image/png", png).andExpect(status().isOk()).andExpect(jsonPath("$.hasCover").value(true));
        get(t, "/api/subjects/" + draft + "/cover").andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        get(student(), "/api/subjects/" + draft + "/cover").andExpect(status().isNotFound()); // draft: hidden
        post(t, "/api/teacher/subjects/" + draft + "/modules", Map.of("title", "m")).andExpect(status().isCreated());
        upload(teacher(), "/api/teacher/subjects/" + draft + "/cover", "c.png", "image/png", png).andExpect(status().isNotFound());
    }
}
