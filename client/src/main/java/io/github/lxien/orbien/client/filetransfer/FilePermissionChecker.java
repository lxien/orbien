package io.github.lxien.orbien.client.filetransfer;

import io.github.lxien.orbien.core.message.Message;

import java.nio.file.Path;

public final class FilePermissionChecker {

    private FilePermissionChecker() {
    }

    public static Path resolveSafe(Path rootPath, String requestPath) {
        if (rootPath == null) {
            throw new FileAccessException("文件共享根目录未配置");
        }
        Path root = rootPath.toAbsolutePath().normalize();
        String normalized = requestPath == null || requestPath.isBlank() ? "/" : requestPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        String relativeStr = "/".equals(normalized) ? "" : normalized.substring(1);
        if (relativeStr.contains("..")) {
            throw new FileAccessException("非法路径");
        }
        Path relative = relativeStr.isEmpty() ? Path.of("") : Path.of(relativeStr.replace('\\', '/'));
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new FileAccessException("路径越界");
        }
        return resolved;
    }

    public static void assertWritable(Message.FileShareLimits limits) {
        if (limits != null && !limits.getAllowUpload()) {
            throw new FileAccessException("不允许上传");
        }
    }

    public static void assertDeletable(Message.FileShareLimits limits) {
        if (limits != null && !limits.getAllowDelete()) {
            throw new FileAccessException("不允许删除");
        }
    }

    public static void assertMkdir(Message.FileShareLimits limits) {
        if (limits != null && !limits.getAllowMkdir()) {
            throw new FileAccessException("不允许创建目录");
        }
    }

    public static long maxUploadSize(Message.FileShareLimits limits) {
        if (limits == null || limits.getMaxUploadSize() <= 0) {
            return io.github.lxien.orbien.core.domain.FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE;
        }
        return limits.getMaxUploadSize();
    }

    public static class FileAccessException extends RuntimeException {
        public FileAccessException(String message) {
            super(message);
        }
    }
}
