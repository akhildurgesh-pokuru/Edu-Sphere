package com.edusphere.storage;

import com.edusphere.common.ApiException;
import com.edusphere.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {
    private final Path root;

    public LocalStorageService(AppProperties props) {
        this.root = Path.of(props.storagePath()).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream in, String extension) throws IOException {
        if (!extension.matches("[a-z0-9]{1,5}")) throw ApiException.badRequest("Unsupported file type");
        LocalDate d = LocalDate.now();
        String key = d.getYear() + "/" + String.format("%02d", d.getMonthValue()) + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------")); // never executable
        } catch (UnsupportedOperationException ignored) {
            // non-POSIX file system
        }
        return key;
    }

    @Override
    public Resource load(String key) {
        Path p = resolve(key);
        if (!Files.isRegularFile(p)) throw ApiException.notFound("File not found");
        return new FileSystemResource(p);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // best effort
        }
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw ApiException.badRequest("Invalid file path");
        return p;
    }
}
