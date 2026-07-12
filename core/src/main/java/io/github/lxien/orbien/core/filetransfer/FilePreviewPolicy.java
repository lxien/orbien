package io.github.lxien.orbien.core.filetransfer;

import java.util.Locale;
import java.util.Set;

/**
 * 文件预览类型判定与大小限制
 */
public final class FilePreviewPolicy {

    public static final long MAX_TEXT_BYTES = 512 * 1024;
    public static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    public enum Kind {
        FOLDER, IMAGE, TEXT, UNSUPPORTED
    }

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "jpe", "jfif", "png", "gif", "bmp", "webp", "ico",
            "tiff", "tif", "heic", "heif", "raw", "svg", "avif", "jp2", "j2k", "psd"
    );

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "log", "md", "markdown", "json", "yaml", "yml", "xml", "csv",
            "js", "mjs", "cjs", "jsx", "ts", "tsx", "java", "py", "go", "rs", "rb",
            "php", "swift", "kt", "kts", "css", "scss", "sass", "less", "sh", "bash",
            "zsh", "bat", "cmd", "ps1", "properties", "ini", "conf", "cfg", "env",
            "vue", "svelte", "sql", "toml", "gradle", "dockerfile", "makefile",
            "h", "hpp", "c", "cc", "cpp", "cs", "pl", "r", "lua", "vim", "ex", "exs",
            "dart", "zig", "mm", "m", "vmoptions", "gitignore", "gitattributes",
            "editorconfig", "proto", "mod", "sum", "lock"
    );

    private FilePreviewPolicy() {
    }

    public record Decision(
            Kind kind,
            boolean previewable,
            long maxBytes,
            String mime,
            String reason
    ) {
        public static Decision folder() {
            return new Decision(Kind.FOLDER, false, 0, null, null);
        }
    }

    public static Decision resolve(String filename, long size, boolean directory) {
        if (directory) {
            return Decision.folder();
        }
        String ext = extension(filename);
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return imageDecision(size);
        }
        if (TEXT_EXTENSIONS.contains(ext)) {
            return textDecision(size);
        }
        return new Decision(Kind.UNSUPPORTED, false, 0, null, "TYPE_NOT_ALLOWED");
    }

    private static Decision imageDecision(long size) {
        if (size > MAX_IMAGE_BYTES) {
            return new Decision(Kind.IMAGE, false, MAX_IMAGE_BYTES, "image/*", "FILE_TOO_LARGE");
        }
        return new Decision(Kind.IMAGE, true, MAX_IMAGE_BYTES, "image/*", null);
    }

    private static Decision textDecision(long size) {
        if (size > MAX_TEXT_BYTES) {
            return new Decision(Kind.TEXT, false, MAX_TEXT_BYTES, "text/plain; charset=utf-8", "FILE_TOO_LARGE");
        }
        return new Decision(Kind.TEXT, true, MAX_TEXT_BYTES, "text/plain; charset=utf-8", null);
    }

    public static String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String mimeForFilename(String filename) {
        String ext = extension(filename);
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return switch (ext) {
                case "jpg", "jpe", "jfif" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "bmp" -> "image/bmp";
                case "webp" -> "image/webp";
                case "ico" -> "image/x-icon";
                case "svg" -> "image/svg+xml";
                case "avif" -> "image/avif";
                case "tiff", "tif" -> "image/tiff";
                case "heic" -> "image/heic";
                case "heif" -> "image/heif";
                default -> "image/*";
            };
        }
        if (TEXT_EXTENSIONS.contains(ext)) {
            return "text/plain; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
