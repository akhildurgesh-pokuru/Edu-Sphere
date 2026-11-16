package com.edusphere.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusphere.common.ApiException;
import com.edusphere.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageServiceTest {
    @TempDir Path dir;

    private LocalStorageService service() {
        return new LocalStorageService(new AppProperties("", "", dir.toString(), false, true, null, null));
    }

    @Test
    void storesUnderRandomNamesAndNeverExecutable() throws Exception {
        LocalStorageService s = service();
        String k1 = s.store(new ByteArrayInputStream("a".getBytes()), "pdf");
        String k2 = s.store(new ByteArrayInputStream("a".getBytes()), "pdf");
        assertThat(k1).isNotEqualTo(k2).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.pdf");
        Path p = dir.resolve(k1);
        assertThat(Files.readString(p)).isEqualTo("a");
        assertThat(Files.isExecutable(p)).isFalse();
        s.delete(k1);
        assertThat(Files.exists(p)).isFalse();
    }

    @Test
    void refusesPathTraversalAndOddExtensions() {
        LocalStorageService s = service();
        assertThatThrownBy(() -> s.load("../../etc/passwd")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> s.store(new ByteArrayInputStream(new byte[1]), "../x")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> s.store(new ByteArrayInputStream(new byte[1]), "PDF")).isInstanceOf(ApiException.class);
    }
}
