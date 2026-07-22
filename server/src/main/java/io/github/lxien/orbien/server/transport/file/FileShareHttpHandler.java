package io.github.lxien.orbien.server.transport.file;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.lxien.orbien.core.domain.FileShareAuthConfig;
import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.filetransfer.FilePreviewPolicy;
import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.server.filetransfer.FileTransferCoordinator;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileShareHttpHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FileShareHttpHandler.class);
    private static final Gson GSON = new Gson();
    private static final Pattern SESSION_COOKIE = Pattern.compile("orbien_file_session=([^;\\s]+)");

    @Autowired
    private FileAuthService fileAuthService;
    @Autowired
    private FileTransferCoordinator fileTransferCoordinator;

    private volatile String portalHtml;
    private final Map<String, byte[]> staticResourceCache = new ConcurrentHashMap<>();

    public void handle(ChannelHandlerContext ctx, Object msg, ProxyConfigExt ext) {
        if (!(msg instanceof ByteBuf buf)) {
            ReferenceCountUtil.release(msg);
            return;
        }
        try {
            SimpleHttpRequest req = SimpleHttpRequest.parse(buf);
            if (req == null) {
                logger.warn("无法解析文件共享 HTTP 请求, bytes={}", buf.readableBytes());
                NettyHttpUtils.sendHttp400(ctx.channel());
                return;
            }
            ProxyConfig config = ext.getProxyConfig();
            String requestPath = req.path();
            String path = stripQuery(requestPath);
            String sessionId = parseSession(req.header("Cookie"));

            if ("GET".equals(req.method()) && ("/".equals(path) || "/index.html".equals(path))) {
                servePortal(ctx.channel());
                return;
            }
            if ("GET".equals(req.method()) && serveStaticResource(ctx.channel(), path)) {
                return;
            }
            if ("POST".equals(req.method()) && "/api/auth/login".equals(path)) {
                handleLogin(ctx.channel(), config, req.bodyText());
                return;
            }
            if ("POST".equals(req.method()) && "/api/auth/logout".equals(path)) {
                fileAuthService.logout(sessionId);
                writeJson(ctx.channel(), 200, Map.of("ok", true));
                return;
            }
            if ("GET".equals(req.method()) && "/api/auth/status".equals(path)) {
                writeAuthStatus(ctx.channel(), config, sessionId);
                return;
            }

            FileAuthService.FileSession session;
            try {
                session = resolveSession(config, sessionId);
            } catch (FileAuthService.AuthException e) {
                writeJson(ctx.channel(), 401, Map.of("message", e.getMessage()));
                return;
            }

            String agentId = config.getAgentId();
            String proxyId = config.getProxyId();

            if ("GET".equals(req.method()) && path.startsWith("/api/files/list")) {
                String dir = queryParam(requestPath, "path");
                String sort = queryParam(requestPath, "sort", "");
                String query = queryParam(requestPath, "q", "");
                Message.FileListResponse resp = fileTransferCoordinator.list(agentId, proxyId, dir, sort, query);
                if (resp.getStatus().getCode() != 0) {
                    logger.debug("文件列表失败 agentId={} proxyId={} path={} msg={}",
                            agentId, proxyId, dir, resp.getStatus().getMessage());
                }
                writeJson(ctx.channel(), resp.getStatus().getCode() == 0 ? 200 : 400, toListJson(resp));
                return;
            }
            if ("POST".equals(req.method()) && path.startsWith("/api/files/mkdir")) {
                if (!session.canWrite()) {
                    writeJson(ctx.channel(), 403, Map.of("message", "只读用户"));
                    return;
                }
                if (!resolveCanMkdir(config, session)) {
                    writeJson(ctx.channel(), 403, Map.of("message", "不允许创建目录"));
                    return;
                }
                JsonObject json = GSON.fromJson(req.bodyText(), JsonObject.class);
                Message.FileOpResponse resp = fileTransferCoordinator.op(agentId, proxyId,
                        FileTransferConstants.OP_MKDIR,
                        json.get("path").getAsString(),
                        json.get("name").getAsString());
                writeJson(ctx.channel(), resp.getStatus().getCode() == 0 ? 200 : 400, toOpJson(resp));
                return;
            }
            if ("POST".equals(req.method()) && path.startsWith("/api/files/move")) {
                handleMove(ctx.channel(), config, session, agentId, proxyId, req.bodyText());
                return;
            }
            if ("POST".equals(req.method()) && path.startsWith("/api/files/rename")) {
                handleRename(ctx.channel(), config, session, agentId, proxyId, req.bodyText());
                return;
            }
            if ("DELETE".equals(req.method()) && path.startsWith("/api/files")) {
                if (!session.canWrite()) {
                    writeJson(ctx.channel(), 403, Map.of("message", "只读用户"));
                    return;
                }
                if (!resolveCanDelete(config, session)) {
                    writeJson(ctx.channel(), 403, Map.of("message", "不允许删除"));
                    return;
                }
                String filePath = queryParam(requestPath, "path");
                Message.FileOpResponse resp = fileTransferCoordinator.op(agentId, proxyId,
                        FileTransferConstants.OP_DELETE, filePath, "",
                        FileTransferConstants.DELETE_MAX_TIMEOUT_MS);
                writeJson(ctx.channel(), resp.getStatus().getCode() == 0 ? 200 : 400, toOpJson(resp));
                return;
            }
            if ("POST".equals(req.method()) && path.startsWith("/api/files/upload")) {
                if (!session.canWrite()) {
                    writeJson(ctx.channel(), 403, Map.of("message", "只读用户"));
                    return;
                }
                if (!resolveCanUpload(config, session)) {
                    writeJson(ctx.channel(), 403, Map.of("message", "不允许上传"));
                    return;
                }
                handleUpload(ctx.channel(), agentId, proxyId, requestPath, req);
                return;
            }
            if ("GET".equals(req.method()) && path.startsWith("/api/files/download")) {
                String filePath = queryParam(requestPath, "path");
                Channel channel = ctx.channel();
                // 下载在后台线程执行，避免在 event loop 上 sync 写出导致死锁
                CompletableFuture.runAsync(() -> {
                    try {
                        handleDownload(channel, config, agentId, proxyId, filePath);
                    } catch (Exception e) {
                        logger.error("文件下载失败 path={}", filePath, e);
                        if (channel.isActive()) {
                            try {
                                writeJson(channel, 503, Map.of("message",
                                        e.getMessage() != null ? e.getMessage() : "下载失败"));
                            } catch (Exception ignored) {
                                channel.close();
                            }
                        }
                    }
                });
                return;
            }
            if ("GET".equals(req.method()) && path.startsWith("/api/files/preview-meta")) {
                String filePath = queryParam(requestPath, "path");
                handlePreviewMeta(ctx.channel(), agentId, proxyId, filePath);
                return;
            }
            if ("GET".equals(req.method()) && path.startsWith("/api/files/preview")) {
                String filePath = queryParam(requestPath, "path");
                handlePreview(ctx.channel(), agentId, proxyId, filePath);
                return;
            }

            NettyHttpUtils.sendHttp404(ctx.channel());
        } catch (IllegalStateException e) {
            logger.warn("文件共享请求失败: {}", e.getMessage());
            writeJson(ctx.channel(), 503, Map.of("message", e.getMessage()));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof TimeoutException) {
                logger.warn("文件共享请求超时: {}", cause.getMessage());
                writeJson(ctx.channel(), 504, Map.of("message", "文件传输超时，请稍后重试或检查网络"));
            } else {
                logger.error("文件共享请求处理失败", e);
                writeJson(ctx.channel(), 503, Map.of("message", cause.getMessage() != null ? cause.getMessage() : "文件传输失败"));
            }
        } catch (TimeoutException e) {
            logger.warn("文件共享请求超时: {}", e.getMessage());
            writeJson(ctx.channel(), 504, Map.of("message", "文件传输超时，请稍后重试或检查网络"));
        } catch (Exception e) {
            logger.error("文件共享请求处理失败", e);
            NettyHttpUtils.sendHttp500(ctx.channel());
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    PreparedUpload prepareUpload(ProxyConfigExt ext, String requestPath, String cookie)
            throws FileAuthService.AuthException, UploadDeniedException {
        ProxyConfig config = ext.getProxyConfig();
        String sessionId = parseSession(cookie);
        FileAuthService.FileSession session = resolveSession(config, sessionId);
        if (!session.canWrite()) {
            throw new UploadDeniedException(403, "只读用户");
        }
        if (!resolveCanUpload(config, session)) {
            throw new UploadDeniedException(403, "不允许上传");
        }
        String dir = queryParam(requestPath, "path");
        return new PreparedUpload(config.getAgentId(), config.getProxyId(), dir);
    }

    void writeJsonHttp(Channel channel, int status, Object body) {
        writeJson(channel, status, body);
    }

    static String parseFilenameFromContentDisposition(String header) {
        Matcher star = FILENAME_STAR.matcher(header);
        if (star.find()) {
            String charset = star.group(1);
            if (charset == null || charset.isBlank()) {
                charset = "UTF-8";
            }
            try {
                return URLDecoder.decode(star.group(2).trim(), charset);
            } catch (Exception e) {
                logger.debug("解析 filename* 失败: {}", e.getMessage());
            }
        }
        Matcher quoted = FILENAME_QUOTED.matcher(header);
        if (quoted.find()) {
            return normalizeFilenameStatic(quoted.group(1));
        }
        Matcher plain = FILENAME_PLAIN.matcher(header);
        if (plain.find()) {
            return normalizeFilenameStatic(plain.group(1).trim());
        }
        return "upload.bin";
    }

    private static String normalizeFilenameStatic(String raw) {
        if (raw == null || raw.isBlank()) {
            return "upload.bin";
        }
        String name = raw.trim();
        if (name.contains("\uFFFD") || looksLikeLatin1MojibakeStatic(name)) {
            name = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.isBlank() ? "upload.bin" : name;
    }

    private static boolean looksLikeLatin1MojibakeStatic(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 0x80 && c <= 0xFF) {
                return true;
            }
        }
        return false;
    }

    record PreparedUpload(String agentId, String proxyId, String dir) {
    }

    static final class UploadDeniedException extends Exception {
        private final int status;

        UploadDeniedException(int status, String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }

    private void writeAuthStatus(Channel channel, ProxyConfig config, String sessionId) {
        FileShareAuthConfig auth = config.getFileShareAuth();
        boolean authRequired = auth != null && auth.isEnabled();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authRequired", authRequired);

        if (!authRequired) {
            FileAuthService.FileSession guest = FileAuthService.FileSession.guest(config.getProxyId());
            body.put("loggedIn", true);
            body.put("username", "访客");
            body.put("permission", guest.permission());
            putFileCapabilities(body, config, guest);
            writeJson(channel, 200, body);
            return;
        }

        if (sessionId != null) {
            try {
                FileAuthService.FileSession session = fileAuthService.syncSession(sessionId, config);
                body.put("loggedIn", true);
                body.put("username", session.username());
                body.put("permission", session.permission());
                putFileCapabilities(body, config, session);
                writeJson(channel, 200, body);
                return;
            } catch (FileAuthService.AuthException e) {
                body.put("loggedIn", false);
                body.put("canWrite", false);
                body.put("message", e.getMessage());
                writeJson(channel, 200, body);
                return;
            }
        }

        body.put("loggedIn", false);
        body.put("canWrite", false);
        writeJson(channel, 200, body);
    }

    private void putFileCapabilities(Map<String, Object> body, ProxyConfig config, FileAuthService.FileSession session) {
        body.put("canUpload", resolveCanUpload(config, session));
        body.put("canDelete", resolveCanDelete(config, session));
        body.put("canMkdir", resolveCanMkdir(config, session));
        body.put("canMove", resolveCanMove(config, session));
        body.put("canRename", resolveCanRename(config, session));
        body.put("canWrite", resolveCanWrite(config, session));
    }

    private boolean resolveCanUpload(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            return config.getFileShareLimits().isAllowUpload();
        }
        return true;
    }

    private boolean resolveCanDelete(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            return config.getFileShareLimits().isAllowDelete();
        }
        return true;
    }

    private boolean resolveCanWrite(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            FileShareLimitsConfig limits = config.getFileShareLimits();
            return limits.isAllowUpload() || limits.isAllowDelete() || limits.isAllowMkdir()
                    || limits.isAllowMove() || limits.isAllowRename();
        }
        return true;
    }

    private boolean resolveCanMkdir(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            return config.getFileShareLimits().isAllowMkdir();
        }
        return true;
    }

    private boolean resolveCanMove(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            return config.getFileShareLimits().isAllowMove();
        }
        return true;
    }

    private boolean resolveCanRename(ProxyConfig config, FileAuthService.FileSession session) {
        if (session == null || !session.canWrite()) {
            return false;
        }
        if (config.hasFileShareLimits()) {
            return config.getFileShareLimits().isAllowRename();
        }
        return true;
    }

    private void handleMove(Channel channel, ProxyConfig config, FileAuthService.FileSession session,
                            String agentId, String proxyId, String bodyText) throws Exception {
        if (!session.canWrite()) {
            writeJson(channel, 403, Map.of("message", "只读用户"));
            return;
        }
        if (!resolveCanMove(config, session)) {
            writeJson(channel, 403, Map.of("message", "不允许移动文件"));
            return;
        }
        JsonObject json = GSON.fromJson(bodyText, JsonObject.class);
        if (json == null || !json.has("destDir") || !json.has("items") || !json.get("items").isJsonArray()) {
            writeJson(channel, 400, Map.of("message", "请求参数无效"));
            return;
        }
        String destDir = json.get("destDir").getAsString();
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        for (var element : json.getAsJsonArray("items")) {
            String itemPath = element.getAsString();
            try {
                Message.FileOpResponse resp = fileTransferCoordinator.op(agentId, proxyId,
                        FileTransferConstants.OP_MOVE, itemPath, destDir,
                        FileTransferConstants.MOVE_MAX_TIMEOUT_MS);
                if (resp.getStatus().getCode() == 0) {
                    successCount++;
                    results.add(resultItem(itemPath, true, null));
                } else {
                    failCount++;
                    results.add(resultItem(itemPath, false, resp.getStatus().getMessage()));
                }
            } catch (Exception e) {
                failCount++;
                results.add(resultItem(itemPath, false, e.getMessage() != null ? e.getMessage() : "移动失败"));
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("successCount", successCount);
        payload.put("failCount", failCount);
        payload.put("results", results);
        writeJson(channel, 200, payload);
    }

    private void handleRename(Channel channel, ProxyConfig config, FileAuthService.FileSession session,
                              String agentId, String proxyId, String bodyText) throws Exception {
        if (!session.canWrite()) {
            writeJson(channel, 403, Map.of("message", "只读用户"));
            return;
        }
        if (!resolveCanRename(config, session)) {
            writeJson(channel, 403, Map.of("message", "不允许重命名"));
            return;
        }
        JsonObject json = GSON.fromJson(bodyText, JsonObject.class);
        if (json == null || !json.has("path") || !json.has("name")) {
            writeJson(channel, 400, Map.of("message", "请求参数无效"));
            return;
        }
        Message.FileOpResponse resp = fileTransferCoordinator.op(agentId, proxyId,
                FileTransferConstants.OP_RENAME,
                json.get("path").getAsString(),
                json.get("name").getAsString());
        writeJson(channel, resp.getStatus().getCode() == 0 ? 200 : 400, toOpJson(resp));
    }

    private Map<String, Object> resultItem(String path, boolean ok, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", path);
        item.put("ok", ok);
        if (!ok && message != null) {
            item.put("message", message);
        }
        return item;
    }

    private FileAuthService.FileSession resolveSession(ProxyConfig config, String sessionId) {
        FileShareAuthConfig auth = config.getFileShareAuth();
        if (auth == null || !auth.isEnabled()) {
            return FileAuthService.FileSession.guest(config.getProxyId());
        }
        return fileAuthService.syncSession(sessionId, config);
    }

    private void handleLogin(Channel channel, ProxyConfig config, String body) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            String username = json.get("username").getAsString();
            String sessionId = fileAuthService.login(config, username, json.get("password").getAsString());
            FileAuthService.FileSession session = fileAuthService.syncSession(sessionId, config);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ok", true);
            payload.put("username", username);
            payload.put("permission", session.permission());
            putFileCapabilities(payload, config, session);
            String response = GSON.toJson(payload);
            byte[] bodyBytes = response.getBytes(StandardCharsets.UTF_8);
            String http = """
                    HTTP/1.1 200 OK\r
                    Content-Type: application/json\r
                    Set-Cookie: orbien_file_session=%s; Path=/; HttpOnly\r
                    Content-Length: %d\r
                    Connection: close\r
                    \r
                    """.formatted(sessionId, bodyBytes.length);
            channel.write(Unpooled.copiedBuffer(http, CharsetUtil.UTF_8));
            channel.writeAndFlush(Unpooled.wrappedBuffer(bodyBytes));
        } catch (FileAuthService.AuthException e) {
            writeJson(channel, 401, Map.of("message", e.getMessage()));
        }
    }

    private void handleUpload(Channel channel, String agentId, String proxyId, String path, SimpleHttpRequest req) throws Exception {
        String dir = queryParam(path, "path");
        MultipartFilePart file = extractMultipartFile(req);
        if (file == null) {
            writeJson(channel, 400, Map.of("message", "无效的上传"));
            return;
        }
        String filename = file.filename();
        String filePath = "/".equals(dir) || dir.isEmpty() ? "/" + filename : (dir.endsWith("/") ? dir + filename : dir + "/" + filename);
        FileTransferCoordinator.UploadSession uploadSession =
                fileTransferCoordinator.startUpload(agentId, proxyId, filePath, file.length());
        String requestId = uploadSession.requestId();
        int chunkSize = FileTransferConstants.CHUNK_SIZE;
        long offset = 0;
        for (int i = 0; i < file.length(); i += chunkSize) {
            int len = Math.min(chunkSize, file.length() - i);
            boolean last = i + len >= file.length();
            fileTransferCoordinator.uploadChunk(agentId, requestId, offset, file.data(), file.offset() + i, len, last);
            offset += len;
        }
        fileTransferCoordinator.awaitUpload(uploadSession);
        writeJson(channel, 200, Map.of("ok", true));
    }

    private static final Pattern FILENAME_STAR = Pattern.compile("filename\\*=([^']*)''([^;\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME_QUOTED = Pattern.compile("filename=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME_PLAIN = Pattern.compile("filename=([^;\\r\\n]+)", Pattern.CASE_INSENSITIVE);

    private void handleDownload(Channel channel, ProxyConfig config, String agentId, String proxyId,
                                String filePath) throws Exception {
        Message.FileEntry entry = findFileEntry(agentId, proxyId, filePath);
        if (entry == null) {
            writeJson(channel, 404, Map.of("message", "文件不存在"));
            return;
        }
        if (entry.getDirectory()) {
            writeJson(channel, 400, Map.of("message", "目录不支持下载"));
            return;
        }
        long maxSize = resolveMaxTransferSize(config);
        if (entry.getSize() > maxSize) {
            writeJson(channel, 413, Map.of("message", "文件超过大小限制"));
            return;
        }

        String filename = entry.getName();
        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String disposition = buildContentDisposition(filename);
        String header = """
                HTTP/1.1 200 OK\r
                Content-Type: %s\r
                Content-Disposition: %s\r
                Content-Length: %d\r
                Connection: close\r
                \r
                """.formatted(contentType, disposition, entry.getSize());
        channel.writeAndFlush(Unpooled.copiedBuffer(header, CharsetUtil.UTF_8)).sync();

        try {
            fileTransferCoordinator.download(agentId, proxyId, filePath, 0, entry.getSize(), data -> {
                if (!channel.isActive()) {
                    throw new IllegalStateException("下载连接已关闭");
                }
                channel.writeAndFlush(Unpooled.wrappedBuffer(data)).sync();
            });
        } catch (Exception e) {
            // 响应头已发出，只能关闭连接，避免再写 JSON 破坏 HTTP 帧
            logger.warn("文件下载中断 path={} msg={}", filePath, e.getMessage());
            if (channel.isActive()) {
                channel.close();
            }
        }
    }

    private long resolveMaxTransferSize(ProxyConfig config) {
        if (config.hasFileShareLimits()) {
            Long max = config.getFileShareLimits().getMaxUploadSize();
            if (max != null && max > 0) {
                return max;
            }
        }
        return FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE;
    }

    private void handlePreviewMeta(Channel channel, String agentId, String proxyId, String filePath) throws Exception {
        Message.FileEntry entry = findFileEntry(agentId, proxyId, filePath);
        if (entry == null) {
            writeJson(channel, 404, Map.of("message", "文件不存在"));
            return;
        }
        String filename = entry.getName();
        FilePreviewPolicy.Decision decision = FilePreviewPolicy.resolve(
                filename, entry.getSize(), entry.getDirectory());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("path", filePath);
        body.put("name", filename);
        body.put("directory", entry.getDirectory());
        body.put("size", entry.getSize());
        body.put("modifiedTime", entry.getModifiedTime());
        body.put("createdTime", entry.getCreatedTime());
        body.put("lastAccessTime", entry.getLastAccessTime());
        body.put("previewKind", decision.kind().name().toLowerCase(Locale.ROOT));
        body.put("previewable", decision.previewable());
        body.put("maxPreviewBytes", decision.maxBytes());
        body.put("mime", decision.mime() != null ? decision.mime() : FilePreviewPolicy.mimeForFilename(filename));
        if (decision.reason() != null) {
            body.put("reason", decision.reason());
        }
        writeJson(channel, 200, body);
    }

    private void handlePreview(Channel channel, String agentId, String proxyId, String filePath) throws Exception {
        Message.FileEntry entry = findFileEntry(agentId, proxyId, filePath);
        if (entry == null) {
            writeJson(channel, 404, Map.of("message", "文件不存在"));
            return;
        }
        if (entry.getDirectory()) {
            writeJson(channel, 400, Map.of("message", "目录不支持预览"));
            return;
        }
        FilePreviewPolicy.Decision decision = FilePreviewPolicy.resolve(
                entry.getName(), entry.getSize(), false);
        if (!decision.previewable()) {
            writeJson(channel, 415, Map.of("message", previewReasonMessage(decision.reason())));
            return;
        }
        byte[] data = fileTransferCoordinator.download(agentId, proxyId, filePath, decision.maxBytes());
        String filename = entry.getName();
        String mime = FilePreviewPolicy.mimeForFilename(filename);
        boolean truncated = entry.getSize() > data.length;
        writeInlineBinary(channel, mime, filename, data, truncated);
    }

    private Message.FileEntry findFileEntry(String agentId, String proxyId, String filePath) throws Exception {
        if (filePath == null || filePath.isBlank() || "/".equals(filePath)) {
            return null;
        }
        String parent = parentDirOf(filePath);
        String name = filenameFromPath(filePath);
        Message.FileListResponse resp = fileTransferCoordinator.list(agentId, proxyId, parent, "");
        if (resp.getStatus().getCode() != 0) {
            throw new IllegalStateException(resp.getStatus().getMessage());
        }
        for (Message.FileEntry entry : resp.getEntriesList()) {
            if (name.equals(entry.getName())) {
                return entry;
            }
        }
        return null;
    }

    private String parentDirOf(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "/";
        }
        int idx = path.lastIndexOf('/');
        if (idx <= 0) {
            return "/";
        }
        return path.substring(0, idx);
    }

    private String previewReasonMessage(String reason) {
        if ("FILE_TOO_LARGE".equals(reason)) {
            return "文件过大，无法预览";
        }
        if ("TYPE_NOT_ALLOWED".equals(reason)) {
            return "不支持预览此文件类型";
        }
        return "无法预览";
    }

    private void writeInlineBinary(Channel channel, String contentType, String filename,
                                   byte[] data, boolean truncated) {
        String disposition = buildInlineDisposition(filename);
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 200 OK\r\n");
        header.append("Content-Type: ").append(contentType).append("\r\n");
        header.append("Content-Disposition: ").append(disposition).append("\r\n");
        header.append("Content-Length: ").append(data.length).append("\r\n");
        header.append("X-Content-Type-Options: nosniff\r\n");
        header.append("Cache-Control: private, max-age=60\r\n");
        if (truncated) {
            header.append("X-Preview-Truncated: true\r\n");
        }
        header.append("Connection: close\r\n\r\n");
        channel.write(Unpooled.copiedBuffer(header.toString(), CharsetUtil.UTF_8));
        channel.writeAndFlush(Unpooled.wrappedBuffer(data));
    }

    private String buildInlineDisposition(String filename) {
        String asciiFallback = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    private String filenameFromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "download";
        }
        int idx = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String name = idx >= 0 ? filePath.substring(idx + 1) : filePath;
        return name.isBlank() ? "download" : name;
    }

    private String buildContentDisposition(String filename) {
        String asciiFallback = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    private void servePortal(Channel channel) throws Exception {
        if (portalHtml == null) {
            synchronized (this) {
                if (portalHtml == null) {
                    ClassPathResource resource = new ClassPathResource("file-portal/index.html");
                    try (InputStream in = resource.getInputStream()) {
                        portalHtml = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                    }
                }
            }
        }
        NettyHttpUtils.sendHttpResponse(channel, 200, "OK", "text/html; charset=UTF-8", portalHtml);
    }

    private boolean serveStaticResource(Channel channel, String path) {
        String classpath = resolveStaticClasspath(path);
        if (classpath == null) {
            return false;
        }
        byte[] content = staticResourceCache.computeIfAbsent(classpath, this::loadStaticResource);
        if (content == null) {
            NettyHttpUtils.sendHttp404(channel);
            return true;
        }
        NettyHttpUtils.sendHttpBinaryResponse(channel, 200, "OK", staticContentType(path), content);
        return true;
    }

    private String resolveStaticClasspath(String path) {
        if (path == null || path.contains("..")) {
            return null;
        }
        if (path.startsWith("/css/") || path.startsWith("/js/")) {
            return "file-portal" + path;
        }
        if (path.startsWith("/icon/")) {
            String iconPath = URLDecoder.decode(path.substring("/icon/".length()), StandardCharsets.UTF_8);
            if (iconPath.isBlank() || iconPath.contains("..") || iconPath.contains("\\")) {
                return null;
            }
            return "static/icon/" + iconPath;
        }
        if (path.startsWith("/img/")) {
            String imgName = URLDecoder.decode(path.substring("/img/".length()), StandardCharsets.UTF_8);
            if (imgName.isBlank() || imgName.contains("..") || imgName.contains("/") || imgName.contains("\\")) {
                return null;
            }
            return "static/img/" + imgName;
        }
        if ("/favicon.ico".equals(path)) {
            return "static/favicon.ico";
        }
        return null;
    }

    private byte[] loadStaticResource(String classpath) {
        ClassPathResource resource = new ClassPathResource(classpath);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return StreamUtils.copyToByteArray(in);
        } catch (Exception e) {
            logger.warn("加载静态资源失败: {}", classpath, e);
            return null;
        }
    }

    private String staticContentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    private String stripQuery(String path) {
        if (path == null) {
            return "/";
        }
        int idx = path.indexOf('?');
        return idx >= 0 ? path.substring(0, idx) : path;
    }

    private Map<String, Object> toOpJson(Message.FileOpResponse resp) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (resp.hasStatus()) {
            map.put("status", Map.of("code", resp.getStatus().getCode(), "message", resp.getStatus().getMessage()));
        }
        return map;
    }

    private Map<String, Object> toListJson(Message.FileListResponse resp) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Message.FileEntry e : resp.getEntriesList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getName());
            item.put("directory", e.getDirectory());
            item.put("size", e.getSize());
            item.put("modifiedTime", e.getModifiedTime());
            item.put("createdTime", e.getCreatedTime());
            item.put("lastAccessTime", e.getLastAccessTime());
            entries.add(item);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("currentPath", resp.getCurrentPath());
        map.put("parentPath", resp.getParentPath());
        map.put("entries", entries);
        if (resp.hasStatus()) {
            map.put("status", Map.of("code", resp.getStatus().getCode(), "message", resp.getStatus().getMessage()));
        }
        return map;
    }

    private void writeJson(Channel channel, int status, Object body) {
        String json = GSON.toJson(body);
        NettyHttpUtils.sendHttpResponse(channel, status, status == 200 ? "OK" : "Error",
                "application/json; charset=UTF-8", json);
    }

    private String parseSession(String cookie) {
        if (cookie == null) {
            return null;
        }
        Matcher m = SESSION_COOKIE.matcher(cookie);
        return m.find() ? m.group(1) : null;
    }

    private String queryParam(String path, String key) {
        return queryParam(path, key, "/");
    }

    private String queryParam(String path, String key, String defaultValue) {
        int idx = path.indexOf('?');
        if (idx < 0) {
            return defaultValue;
        }
        String query = path.substring(idx + 1);
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }

    private record MultipartFilePart(byte[] data, int offset, int length, String filename) {
    }

    private MultipartFilePart extractMultipartFile(SimpleHttpRequest req) {
        String ct = req.header("Content-Type");
        if (ct == null || !ct.contains("multipart/form-data")) {
            return null;
        }
        String boundary = extractBoundary(ct);
        if (boundary == null) {
            return null;
        }
        byte[] body = req.bodyBytes();
        byte[] boundaryMarker = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        int searchFrom = 0;
        while (searchFrom < body.length) {
            int partStart = indexOf(body, boundaryMarker, searchFrom);
            if (partStart < 0) {
                break;
            }
            partStart += boundaryMarker.length;
            if (partStart + 1 < body.length && body[partStart] == '-' && body[partStart + 1] == '-') {
                break;
            }
            if (partStart + 1 < body.length && (body[partStart] == '\r' || body[partStart] == '\n')) {
                partStart = skipLineBreak(body, partStart);
            }
            int headerEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), partStart);
            if (headerEnd < 0) {
                break;
            }
            String partHeader = new String(body, partStart, headerEnd - partStart, StandardCharsets.UTF_8);
            if (partHeader.contains("filename=") || partHeader.contains("filename*=")) {
                int contentStart = headerEnd + 4;
                int contentEnd = indexOf(body, boundaryMarker, contentStart);
                if (contentEnd < 0) {
                    contentEnd = body.length;
                }
                if (contentEnd - 2 >= contentStart && body[contentEnd - 2] == '\r' && body[contentEnd - 1] == '\n') {
                    contentEnd -= 2;
                }
                if (contentEnd < contentStart) {
                    return null;
                }
                return new MultipartFilePart(
                        body,
                        contentStart,
                        contentEnd - contentStart,
                        parseFilenameFromContentDisposition(partHeader));
            }
            searchFrom = headerEnd + 4;
        }
        return null;
    }

    private String extractBoundary(String contentType) {
        int idx = contentType.indexOf("boundary=");
        if (idx < 0) {
            return null;
        }
        String boundary = contentType.substring(idx + "boundary=".length()).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        int semi = boundary.indexOf(';');
        if (semi >= 0) {
            boundary = boundary.substring(0, semi).trim();
        }
        return boundary.isEmpty() ? null : boundary;
    }

    private int indexOf(byte[] source, byte[] target, int from) {
        outer:
        for (int i = from; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private int skipLineBreak(byte[] body, int pos) {
        if (pos < body.length && body[pos] == '\r') {
            pos++;
        }
        if (pos < body.length && body[pos] == '\n') {
            pos++;
        }
        return pos;
    }

    static class SimpleHttpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> headers = new HashMap<>();
        private final byte[] bodyBytes;

        SimpleHttpRequest(String method, String path, byte[] bodyBytes) {
            this.method = method;
            this.path = path;
            this.bodyBytes = bodyBytes == null ? new byte[0] : bodyBytes;
        }

        String method() { return method; }
        String path() { return path; }
        byte[] bodyBytes() { return bodyBytes; }
        String bodyText() { return new String(bodyBytes, StandardCharsets.UTF_8); }
        String header(String name) { return headers.get(name.toLowerCase()); }

        static SimpleHttpRequest parse(ByteBuf buf) {
            int readerIndex = buf.readerIndex();
            int readable = buf.readableBytes();
            if (readable <= 0 || readable > Integer.MAX_VALUE / 2) {
                return null;
            }
            int headerEnd = indexOf(buf, readerIndex, readable, CRLFCRLF);
            if (headerEnd < 0) {
                return null;
            }
            int headerLen = headerEnd - readerIndex;
            String headerPart = buf.toString(readerIndex, headerLen, StandardCharsets.US_ASCII);
            int bodyStart = headerEnd + 4;
            int bodyLen = readerIndex + readable - bodyStart;
            byte[] body = bodyLen > 0 ? new byte[bodyLen] : EMPTY_BODY;
            if (bodyLen > 0) {
                buf.getBytes(bodyStart, body);
            }
            String[] lines = headerPart.split("\r\n");
            if (lines.length == 0) {
                return null;
            }
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length < 2) {
                return null;
            }
            SimpleHttpRequest req = new SimpleHttpRequest(requestLine[0], requestLine[1], body);
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    req.headers.put(lines[i].substring(0, colon).trim().toLowerCase(),
                            lines[i].substring(colon + 1).trim());
                }
            }
            return req;
        }

        private static final byte[] EMPTY_BODY = new byte[0];
        private static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

        private static int indexOf(ByteBuf buf, int start, int readable, byte[] target) {
            int end = start + readable - target.length;
            outer:
            for (int i = start; i <= end; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (buf.getByte(i + j) != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
    }
}
