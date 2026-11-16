package com.edusphere.storage;

import com.edusphere.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Whitelists file types, limits sizes and checks the real content (magic bytes), not just the extension. */
@Component
public class FileValidator {
    public record FileInfo(String originalName, String extension, String kind, String contentType, long size) {}

    private record Type(String kind, String contentType, long maxBytes) {}

    private static final long MB = 1024L * 1024L;
    private static final Map<String, Type> TYPES = Map.ofEntries(
            Map.entry("mp4", new Type("VIDEO", "video/mp4", 500 * MB)),
            Map.entry("webm", new Type("VIDEO", "video/webm", 500 * MB)),
            Map.entry("pdf", new Type("PDF", "application/pdf", 100 * MB)),
            Map.entry("ppt", new Type("DOCUMENT", "application/vnd.ms-powerpoint", 100 * MB)),
            Map.entry("pptx", new Type("DOCUMENT", "application/vnd.openxmlformats-officedocument.presentationml.presentation", 100 * MB)),
            Map.entry("doc", new Type("DOCUMENT", "application/msword", 100 * MB)),
            Map.entry("docx", new Type("DOCUMENT", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100 * MB)),
            Map.entry("txt", new Type("DOCUMENT", "text/plain", 20 * MB)),
            Map.entry("jpg", new Type("IMAGE", "image/jpeg", 5 * MB)),
            Map.entry("jpeg", new Type("IMAGE", "image/jpeg", 5 * MB)),
            Map.entry("png", new Type("IMAGE", "image/png", 5 * MB)),
            Map.entry("gif", new Type("IMAGE", "image/gif", 5 * MB)),
            Map.entry("webp", new Type("IMAGE", "image/webp", 5 * MB)));

    public static final Set<String> MATERIAL_EXT = Set.of("mp4", "webm", "pdf", "ppt", "pptx", "doc", "docx", "txt");
    public static final Set<String> DOCUMENT_EXT = Set.of("pdf", "ppt", "pptx", "doc", "docx", "txt");
    public static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    public FileInfo validate(MultipartFile file, Set<String> allowedExt) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Choose a file to upload");
        String name = sanitizeName(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        Type t = TYPES.get(ext);
        if (t == null || !allowedExt.contains(ext)) {
            throw ApiException.badRequest("This file type is not allowed. Allowed: " + String.join(", ", allowedExt.stream().sorted().toList()));
        }
        if (file.getSize() > t.maxBytes()) {
            throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,
                    "The file is too large (max " + (t.maxBytes() / MB) + " MB for this type)");
        }
        byte[] head = new byte[4096];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw ApiException.badRequest("The file could not be read");
        }
        if (!contentMatches(ext, head, n)) {
            throw ApiException.badRequest("The file content does not match its extension");
        }
        return new FileInfo(name, ext, t.kind(), t.contentType(), file.getSize());
    }

    static String sanitizeName(String original) {
        String n = original == null ? "file" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}<>:\"|?*]", "_").trim();
        if (n.isEmpty()) n = "file";
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }

    private static boolean starts(byte[] h, int n, int... sig) {
        if (n < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if ((h[i] & 0xFF) != sig[i]) return false;
        return true;
    }

    private static boolean noNul(byte[] h, int n) {
        for (int i = 0; i < n; i++) if (h[i] == 0) return false;
        return true;
    }

    private static boolean contentMatches(String ext, byte[] h, int n) {
        return switch (ext) {
            case "mp4" -> n >= 12 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
            case "webm" -> starts(h, n, 0x1A, 0x45, 0xDF, 0xA3);
            case "pdf" -> starts(h, n, '%', 'P', 'D', 'F');
            case "docx", "pptx" -> starts(h, n, 0x50, 0x4B, 0x03, 0x04);
            case "doc", "ppt" -> starts(h, n, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "jpg", "jpeg" -> starts(h, n, 0xFF, 0xD8, 0xFF);
            case "png" -> starts(h, n, 0x89, 'P', 'N', 'G');
            case "gif" -> starts(h, n, 'G', 'I', 'F', '8');
            case "webp" -> n >= 12 && starts(h, n, 'R', 'I', 'F', 'F') && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case "txt" -> n > 0 && noNul(h, n);
            default -> false;
        };
    }
}
