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
                try (Stream<Path> children = Files.list(target)) {
                    if (children.findAny().isPresent()) {
                        throw new FilePermissionChecker.FileAccessException("只能删除空目录");
                    }
                }
                Files.delete(target);
            } else {
                Files.delete(target);
            }
        } catch (NoSuchFileException e) {
            throw new FilePermissionChecker.FileAccessException("文件不存在");
        } catch (DirectoryNotEmptyException e) {
            throw new FilePermissionChecker.FileAccessException("只能删除空目录");
        } catch (AccessDeniedException e) {
            throw new FilePermissionChecker.FileAccessException("没有删除权限");
        }
        return Message.FileOpResponse.newBuilder().setStatus(success()).build();
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
