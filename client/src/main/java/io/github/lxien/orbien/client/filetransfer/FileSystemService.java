package io.github.lxien.orbien.client.filetransfer;

import io.github.lxien.orbien.core.message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileSystemService {

    public Message.FileListResponse list(Path rootPath, String requestPath) throws IOException {
        Path dir = FilePermissionChecker.resolveSafe(rootPath, requestPath);
        if (!Files.exists(dir)) {
            throw new FilePermissionChecker.FileAccessException("目录不存在");
        }
        if (!Files.isDirectory(dir)) {
            throw new FilePermissionChecker.FileAccessException("不是目录");
        }
        String current = toRelative(rootPath, dir);
        String parent = "/".equals(current) ? "" : parentPath(current);

        List<Message.FileEntry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted(Comparator.<Path>comparingInt(p -> Files.isDirectory(p) ? 0 : 1)
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(path -> entries.add(toEntry(path)));
        } catch (NoSuchFileException | NotDirectoryException e) {
            throw new FilePermissionChecker.FileAccessException("目录不存在");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有读取权限");
        }
        return Message.FileListResponse.newBuilder()
                .setCurrentPath(current)
                .setParentPath(parent)
                .addAllEntries(entries)
                .setStatus(success())
                .build();
    }

    public Message.FileOpResponse mkdir(Path rootPath, String parentPath, String name,
                                        Message.FileShareLimits limits) throws IOException {
        FilePermissionChecker.assertMkdir(limits);
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")) {
            throw new FilePermissionChecker.FileAccessException("目录名称无效");
        }
        Path parent = FilePermissionChecker.resolveSafe(rootPath, parentPath);
        Path target = parent.resolve(name.trim());
        if (Files.exists(target)) {
            throw new FilePermissionChecker.FileAccessException("目录已存在");
        }
        Files.createDirectory(target);
        return Message.FileOpResponse.newBuilder().setStatus(success()).build();
    }

    public Message.FileOpResponse delete(Path rootPath, String requestPath,
                                         Message.FileShareLimits limits) throws IOException {
        FilePermissionChecker.assertDeletable(limits);
        Path target = FilePermissionChecker.resolveSafe(rootPath, requestPath);
        if (!Files.exists(target)) {
            throw new FilePermissionChecker.FileAccessException("文件不存在");
        }
        try {
            if (Files.isDirectory(target)) {
                deleteDirectoryRecursive(target);
            } else {
                Files.delete(target);
            }
        } catch (NoSuchFileException e) {
            throw new FilePermissionChecker.FileAccessException("文件不存在");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有删除权限");
        } catch (FilePermissionChecker.FileAccessException e) {
            throw e;
        } catch (IOException e) {
            throw new FilePermissionChecker.FileAccessException("删除失败");
        }
        return Message.FileOpResponse.newBuilder().setStatus(success()).build();
    }

    public Message.FileOpResponse move(Path rootPath, String sourcePath, String destDirPath,
                                       Message.FileShareLimits limits) throws IOException {
        FilePermissionChecker.assertMovable(limits);
        Path source = FilePermissionChecker.resolveSafe(rootPath, sourcePath);
        Path destDir = FilePermissionChecker.resolveSafe(rootPath, destDirPath);
        if (!Files.exists(source)) {
            throw new FilePermissionChecker.FileAccessException("源文件不存在");
        }
        if (!Files.exists(destDir) || !Files.isDirectory(destDir)) {
            throw new FilePermissionChecker.FileAccessException("目标目录不存在");
        }
        Path normalizedSource = source.normalize();
        Path normalizedDestDir = destDir.normalize();
        if (normalizedSource.equals(normalizedDestDir)) {
            throw new FilePermissionChecker.FileAccessException("不能移动到自身");
        }
        if (Files.isDirectory(normalizedSource)
                && normalizedDestDir.startsWith(normalizedSource)) {
            throw new FilePermissionChecker.FileAccessException("不能将文件夹移动到其子目录中");
        }
        Path target = normalizedDestDir.resolve(source.getFileName());
        if (Files.exists(target)) {
            throw new FilePermissionChecker.FileAccessException("目标位置已存在同名文件或文件夹");
        }
        Path parent = source.getParent();
        if (parent != null && parent.normalize().equals(normalizedDestDir)) {
            throw new FilePermissionChecker.FileAccessException("文件已在目标目录中");
        }
        try {
            moveEntry(source, target);
        } catch (FileAlreadyExistsException e) {
            throw new FilePermissionChecker.FileAccessException("目标位置已存在同名文件或文件夹");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有移动权限");
        } catch (IOException e) {
            throw new FilePermissionChecker.FileAccessException("移动失败");
        }
        return Message.FileOpResponse.newBuilder().setStatus(success()).build();
    }

    public Message.FileOpResponse rename(Path rootPath, String sourcePath, String newName,
                                         Message.FileShareLimits limits) throws IOException {
        FilePermissionChecker.assertRenamable(limits);
        if (newName == null || newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
            throw new FilePermissionChecker.FileAccessException("名称无效");
        }
        String trimmed = newName.trim();
        if (trimmed.isEmpty() || ".".equals(trimmed) || "..".equals(trimmed)) {
            throw new FilePermissionChecker.FileAccessException("名称无效");
        }
        Path source = FilePermissionChecker.resolveSafe(rootPath, sourcePath);
        if (!Files.exists(source)) {
            throw new FilePermissionChecker.FileAccessException("文件不存在");
        }
        Path parent = source.getParent();
        if (parent == null) {
            throw new FilePermissionChecker.FileAccessException("无法重命名根路径");
        }
        Path target = parent.resolve(trimmed);
        if (source.normalize().equals(target.normalize())) {
            return Message.FileOpResponse.newBuilder().setStatus(success()).build();
        }
        if (Files.exists(target)) {
            throw new FilePermissionChecker.FileAccessException("已存在同名文件或文件夹");
        }
        try {
            moveEntry(source, target);
        } catch (FileAlreadyExistsException e) {
            throw new FilePermissionChecker.FileAccessException("已存在同名文件或文件夹");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有重命名权限");
        } catch (IOException e) {
            throw new FilePermissionChecker.FileAccessException("重命名失败");
        }
        return Message.FileOpResponse.newBuilder().setStatus(success()).build();
    }

    private void moveEntry(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            if (Files.isDirectory(source)) {
                copyDirectory(source, target);
                deleteDirectoryRecursive(source);
            } else {
                Files.move(source, target);
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path destDir = target.resolve(source.relativize(dir));
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path destFile = target.resolve(source.relativize(file));
                if (destFile.getParent() != null) {
                    Files.createDirectories(destFile.getParent());
                }
                Files.copy(file, destFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectoryRecursive(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                try {
                    Files.delete(path);
                } catch (NoSuchFileException ignored) {
                    // 并发删除或重复遍历时忽略
                } catch (AccessDeniedException e) {
                    throw new FilePermissionChecker.FileAccessException("没有删除权限");
                } catch (DirectoryNotEmptyException e) {
                    throw new FilePermissionChecker.FileAccessException("删除失败");
                }
            }
        }
    }

    public void writeChunk(Path rootPath, String requestPath, long offset, byte[] data, boolean last,
                           Message.FileShareLimits limits) {
        FilePermissionChecker.assertWritable(limits);
        long maxUploadSize = FilePermissionChecker.maxUploadSize(limits);
        Path file = FilePermissionChecker.resolveSafe(rootPath, requestPath);
        if (offset == 0 && Files.exists(file) && Files.isDirectory(file)) {
            throw new FilePermissionChecker.FileAccessException("目标是目录");
        }
        Path parent = file.getParent();
        if (parent != null) {
            try {
                if (offset == 0) {
                    Files.createDirectories(parent);
                } else if (!Files.exists(parent) || !Files.isDirectory(parent)) {
                    throw new FilePermissionChecker.FileAccessException("目标目录不存在");
                }
            } catch (IOException e) {
                throw new FilePermissionChecker.FileAccessException("目标目录不存在");
            }
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.position(offset);
            channel.write(ByteBuffer.wrap(data));
            if (last && channel.size() > maxUploadSize) {
                throw new FilePermissionChecker.FileAccessException("文件超过大小限制");
            }
        } catch (FilePermissionChecker.FileAccessException e) {
            deleteQuietly(file);
            throw e;
        } catch (NoSuchFileException e) {
            throw new FilePermissionChecker.FileAccessException("目标目录不存在");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有写入权限");
        } catch (IOException e) {
            deleteQuietly(file);
            throw new FilePermissionChecker.FileAccessException("写入失败");
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    public void cleanupPartialUpload(Path rootPath, String requestPath) {
        try {
            Path file = FilePermissionChecker.resolveSafe(rootPath, requestPath);
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // 清理失败不影响错误响应
        }
    }

    public InputStream openDownload(Path rootPath, String requestPath) throws IOException {
        Path file = FilePermissionChecker.resolveSafe(rootPath, requestPath);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new FilePermissionChecker.FileAccessException("文件不存在");
        }
        return Files.newInputStream(file);
    }

    public long fileSize(Path rootPath, String requestPath) throws IOException {
        Path file = FilePermissionChecker.resolveSafe(rootPath, requestPath);
        return Files.size(file);
    }

    private Message.FileEntry toEntry(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            Message.FileEntry.Builder builder = Message.FileEntry.newBuilder()
                    .setName(path.getFileName().toString())
                    .setDirectory(attrs.isDirectory())
                    .setSize(attrs.isDirectory() ? 0 : attrs.size())
                    .setModifiedTime(attrs.lastModifiedTime().toMillis());
            return builder.build();
        } catch (IOException e) {
            return Message.FileEntry.newBuilder()
                    .setName(path.getFileName().toString())
                    .setDirectory(Files.isDirectory(path))
                    .build();
        }
    }

    private String toRelative(Path root, Path absolute) {
        String rel = root.normalize().relativize(absolute.normalize()).toString().replace('\\', '/');
        return rel.isEmpty() ? "/" : "/" + rel;
    }

    private String parentPath(String current) {
        if ("/".equals(current) || current.isEmpty()) {
            return "";
        }
        int idx = current.lastIndexOf('/');
        if (idx <= 0) {
            return "/";
        }
        return current.substring(0, idx);
    }

    private Message.Status success() {
        return Message.Status.newBuilder().setCode(0).setMessage("ok").build();
    }
}
