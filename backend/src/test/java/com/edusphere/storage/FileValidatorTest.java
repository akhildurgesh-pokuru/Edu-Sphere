package com.edusphere.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusphere.common.ApiException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidatorTest {
    private final FileValidator v = new FileValidator();

    private MockMultipartFile file(String name, byte[] bytes) { return new MockMultipartFile("file", name, "application/octet-stream", bytes); }

    @Test
    void acceptsRealPdfAndReportsServerChosenType() {
        var info = v.validate(file("Lesson 1.pdf", "%PDF-1.7 ...".getBytes()), FileValidator.MATERIAL_EXT);
        assertThat(info.kind()).isEqualTo("PDF");
        assertThat(info.contentType()).isEqualTo("application/pdf");
        assertThat(info.extension()).isEqualTo("pdf");
    }

    @Test
    void rejectsContentThatDoesNotMatchTheExtension() {
        assertThatThrownBy(() -> v.validate(file("a.pdf", "#!/bin/sh".getBytes()), FileValidator.MATERIAL_EXT)).isInstanceOf(ApiException.class).hasMessageContaining("does not match");
        assertThatThrownBy(() -> v.validate(file("a.png", "GIF89a".getBytes()), FileValidator.IMAGE_EXT)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> v.validate(file("a.txt", new byte[]{'h', 0, 'i'}), FileValidator.DOCUMENT_EXT)).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsExecutablesScriptsAndDoubleExtensions() {
        for (String n : new String[]{"run.exe", "x.sh", "x.php", "x.jsp", "x.html", "x.svg", "x.bat", "x.js", "noext", "x.pdf.exe"}) {
            assertThatThrownBy(() -> v.validate(file(n, "%PDF".getBytes()), FileValidator.MATERIAL_EXT)).as(n).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void respectsTheCallersAllowList() {
        assertThatThrownBy(() -> v.validate(file("a.mp4", new byte[]{0, 0, 0, 0, 'f', 't', 'y', 'p', 0, 0, 0, 0}), FileValidator.DOCUMENT_EXT)).isInstanceOf(ApiException.class);
        assertThat(v.validate(file("a.mp4", new byte[]{0, 0, 0, 0, 'f', 't', 'y', 'p', 0, 0, 0, 0}), Set.of("mp4")).kind()).isEqualTo("VIDEO");
    }

    @Test
    void rejectsEmptyFiles() {
        assertThatThrownBy(() -> v.validate(file("a.pdf", new byte[0]), FileValidator.MATERIAL_EXT)).isInstanceOf(ApiException.class);
    }

    @Test
    void sanitizesFilenamesAgainstPathTraversal() {
        assertThat(FileValidator.sanitizeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileValidator.sanitizeName("C:\\Users\\x\\evil.pdf")).isEqualTo("evil.pdf");
        assertThat(FileValidator.sanitizeName("a<b>|c.pdf")).doesNotContain("<", ">", "|");
        assertThat(FileValidator.sanitizeName(null)).isEqualTo("file");
    }
}
