/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.transport.file;

import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import io.github.lxien.orbien.server.filetransfer.FileTransferCoordinator;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class FileShareMultipartUploader {

    private static final int MAX_PREAMBLE_BYTES = 256 * 1024;

    enum Phase {
        FIND_FILE_PART,
        STREAM_FILE,
        DRAIN,
        DONE,
        ERROR
    }

    enum FeedResult {
        NEED_MORE,
        SUCCESS,
        BAD_REQUEST,
        ERROR
    }

    private final FileTransferCoordinator coordinator;
    private final FileShareHttpHandler.PreparedUpload prepared;
    private final long contentLength;
    private final byte[] openBoundary;
    private final byte[] endMarker;
    private final byte[] chunkBuf = new byte[FileTransferConstants.CHUNK_SIZE];

    private Phase phase = Phase.FIND_FILE_PART;
    private final byte[] preamble = new byte[MAX_PREAMBLE_BYTES];
    private int preambleLen;
    private byte[] holdback;
    private int holdbackLen;
    private int chunkLen;
    private long fileOffset;
    private long bodyReceived;
    private FileTransferCoordinator.UploadSession uploadSession;
    private String errorMessage;
    private boolean closed;

    FileShareMultipartUploader(FileTransferCoordinator coordinator,
                               FileShareHttpHandler.PreparedUpload prepared,
                               String contentType,
                               long contentLength) {
        this.coordinator = coordinator;
        this.prepared = prepared;
        this.contentLength = Math.max(0, contentLength);
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            this.openBoundary = null;
            this.endMarker = null;
            this.phase = Phase.ERROR;
            this.errorMessage = "无效的上传";
        } else {
            this.openBoundary = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
            this.endMarker = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);
            this.holdback = new byte[endMarker.length];
        }
    }

    Phase phase() {
        return phase;
    }

    String errorMessage() {
        return errorMessage != null ? errorMessage : "上传失败";
    }

    FeedResult feed(ByteBuf incoming) {
        if (closed || phase == Phase.DONE || phase == Phase.ERROR) {
            ReferenceCountUtil.release(incoming);
            return phase == Phase.DONE ? FeedResult.SUCCESS : FeedResult.ERROR;
        }
        if (incoming == null) {
            return FeedResult.NEED_MORE;
        }
        try {
            while (incoming.isReadable() && phase != Phase.DONE && phase != Phase.ERROR) {
                if (bodyReceived >= contentLength && phase != Phase.STREAM_FILE) {
                    break;
                }
                int take = incoming.readableBytes();
                if (contentLength > 0) {
                    long remain = contentLength - bodyReceived;
                    if (remain <= 0) {
                        break;
                    }
                    take = (int) Math.min(take, remain);
                }
                if (phase == Phase.STREAM_FILE || phase == Phase.FIND_FILE_PART) {
                    take = Math.min(take, FileTransferConstants.CHUNK_SIZE);
                }
                if (take <= 0) {
                    break;
                }
                byte[] slice = new byte[take];
                incoming.readBytes(slice);
                bodyReceived += take;
                FeedResult step = consume(slice, 0, take);
                if (step != FeedResult.NEED_MORE) {
                    return step;
                }
            }
            if (phase == Phase.DRAIN && (contentLength <= 0 || bodyReceived >= contentLength)) {
                return finishSuccess();
            }
            if (phase == Phase.STREAM_FILE && contentLength > 0 && bodyReceived >= contentLength) {
                return fail(FeedResult.BAD_REQUEST, "无效的上传");
            }
            if (phase == Phase.FIND_FILE_PART && contentLength > 0 && bodyReceived >= contentLength) {
                return fail(FeedResult.BAD_REQUEST, "无效的上传");
            }
            return FeedResult.NEED_MORE;
        } catch (OutOfMemoryError oom) {
            fail(FeedResult.ERROR, "内存不足");
            throw oom;
        } catch (Exception e) {
            return fail(FeedResult.ERROR, e.getMessage() != null ? e.getMessage() : "上传失败");
        } finally {
            ReferenceCountUtil.release(incoming);
        }
    }

    void close() {
        closed = true;
        phase = Phase.ERROR;
        holdbackLen = 0;
        chunkLen = 0;
        preambleLen = 0;
        uploadSession = null;
    }

    private FeedResult consume(byte[] data, int offset, int length) throws Exception {
        int pos = offset;
        int end = offset + length;
        while (pos < end && phase != Phase.DONE && phase != Phase.ERROR) {
            switch (phase) {
                case FIND_FILE_PART -> {
                    int room = MAX_PREAMBLE_BYTES - preambleLen;
                    if (room <= 0) {
                        return fail(FeedResult.BAD_REQUEST, "无效的上传");
                    }
                    int n = Math.min(room, end - pos);
                    System.arraycopy(data, pos, preamble, preambleLen, n);
                    preambleLen += n;
                    pos += n;
                    FeedResult found = tryFindFilePart();
                    if (found != FeedResult.NEED_MORE) {
                        return found;
                    }
                }
                case STREAM_FILE -> {
                    int n = end - pos;
                    FeedResult streamed = streamFileBytes(data, pos, n);
                    pos = end;
                    if (streamed != FeedResult.NEED_MORE) {
                        return streamed;
                    }
                }
                case DRAIN -> {
                    pos = end;
                }
                default -> {
                    return phase == Phase.DONE ? FeedResult.SUCCESS : FeedResult.ERROR;
                }
            }
        }
        return FeedResult.NEED_MORE;
    }

    private FeedResult tryFindFilePart() throws Exception {
        int searchFrom = 0;
        while (searchFrom < preambleLen) {
            int partStart = indexOf(preamble, 0, preambleLen, openBoundary, searchFrom);
            if (partStart < 0) {
                if (preambleLen >= MAX_PREAMBLE_BYTES) {
                    return fail(FeedResult.BAD_REQUEST, "无效的上传");
                }
                return FeedResult.NEED_MORE;
            }
            partStart += openBoundary.length;
            if (partStart + 1 < preambleLen && preamble[partStart] == '-' && preamble[partStart + 1] == '-') {
                return fail(FeedResult.BAD_REQUEST, "无效的上传");
            }
            if (partStart < preambleLen && (preamble[partStart] == '\r' || preamble[partStart] == '\n')) {
                partStart = skipLineBreak(preamble, partStart, preambleLen);
            }
            int headerEnd = indexOf(preamble, 0, preambleLen,
                    "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), partStart);
            if (headerEnd < 0) {
                if (preambleLen >= MAX_PREAMBLE_BYTES) {
                    return fail(FeedResult.BAD_REQUEST, "无效的上传");
                }
                return FeedResult.NEED_MORE;
            }
            String partHeader = new String(preamble, partStart, headerEnd - partStart, StandardCharsets.UTF_8);
            int contentStart = headerEnd + 4;
            if (partHeader.contains("filename=") || partHeader.contains("filename*=")) {
                String filename = FileShareHttpHandler.parseFilenameFromContentDisposition(partHeader);
                String dir = prepared.dir();
                String filePath = "/".equals(dir) || dir.isEmpty()
                        ? "/" + filename
                        : (dir.endsWith("/") ? dir + filename : dir + "/" + filename);
                uploadSession = coordinator.startUpload(
                        prepared.agentId(), prepared.proxyId(), filePath, contentLength);
                phase = Phase.STREAM_FILE;
                if (contentStart < preambleLen) {
                    byte[] leftover = Arrays.copyOfRange(preamble, contentStart, preambleLen);
                    preambleLen = 0;
                    return streamFileBytes(leftover, 0, leftover.length);
                }
                preambleLen = 0;
                return FeedResult.NEED_MORE;
            }
            searchFrom = contentStart;
        }
        return FeedResult.NEED_MORE;
    }

    private FeedResult streamFileBytes(byte[] data, int offset, int length) throws Exception {
        if (length <= 0) {
            return FeedResult.NEED_MORE;
        }
        int markerAt = indexOfConcat(holdback, holdbackLen, data, offset, length, endMarker);
        if (markerAt >= 0) {
            emitLogicalRange(holdback, holdbackLen, data, offset, length, 0, markerAt);
            holdbackLen = 0;
            return finishFilePart();
        }

        int total = holdbackLen + length;
        int retain = Math.min(total, endMarker.length - 1);
        int emitTotal = total - retain;
        if (emitTotal > 0) {
            emitLogicalRange(holdback, holdbackLen, data, offset, length, 0, emitTotal);
        }
        copyLogicalRange(holdback, holdbackLen, data, offset, length, emitTotal, retain, this.holdback, 0);
        holdbackLen = retain;
        return FeedResult.NEED_MORE;
    }

    private void emitLogicalRange(byte[] left, int leftLen, byte[] right, int rightOff, int rightLen,
                                  int from, int to) throws Exception {
        int pos = from;
        while (pos < to) {
            int n = Math.min(chunkBuf.length - chunkLen, to - pos);
            copyLogicalRange(left, leftLen, right, rightOff, rightLen, pos, n, chunkBuf, chunkLen);
            chunkLen += n;
            pos += n;
            if (chunkLen == chunkBuf.length) {
                flushChunk(false);
            }
        }
    }

    private static void copyLogicalRange(byte[] left, int leftLen, byte[] right, int rightOff, int rightLen,
                                         int from, int length, byte[] dest, int destOff) {
        int copied = 0;
        while (copied < length) {
            int logical = from + copied;
            if (logical < leftLen) {
                int n = Math.min(length - copied, leftLen - logical);
                System.arraycopy(left, logical, dest, destOff + copied, n);
                copied += n;
            } else {
                int rightIndex = logical - leftLen;
                int n = Math.min(length - copied, rightLen - rightIndex);
                System.arraycopy(right, rightOff + rightIndex, dest, destOff + copied, n);
                copied += n;
            }
        }
    }

    private static int indexOfConcat(byte[] left, int leftLen, byte[] right, int rightOff, int rightLen,
                                     byte[] target) {
        int total = leftLen + rightLen;
        outer:
        for (int i = 0; i <= total - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (byteAt(left, leftLen, right, rightOff, rightLen, i + j) != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte byteAt(byte[] left, int leftLen, byte[] right, int rightOff, int rightLen, int index) {
        if (index < leftLen) {
            return left[index];
        }
        return right[rightOff + (index - leftLen)];
    }

    private FeedResult finishFilePart() throws Exception {
        flushChunk(true);
        coordinator.awaitUpload(uploadSession);
        phase = Phase.DRAIN;
        holdbackLen = 0;
        if (contentLength <= 0 || bodyReceived >= contentLength) {
            return finishSuccess();
        }
        return FeedResult.NEED_MORE;
    }

    private void flushChunk(boolean last) throws Exception {
        if (!last && chunkLen == 0) {
            return;
        }
        if (uploadSession == null) {
            throw new IllegalStateException("上传会话未创建");
        }
        coordinator.uploadChunk(prepared.agentId(), uploadSession.requestId(),
                fileOffset, chunkBuf, 0, chunkLen, last);
        fileOffset += chunkLen;
        chunkLen = 0;
    }

    private FeedResult finishSuccess() {
        phase = Phase.DONE;
        return FeedResult.SUCCESS;
    }

    private FeedResult fail(FeedResult result, String message) {
        phase = Phase.ERROR;
        errorMessage = message;
        return result;
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null || !contentType.contains("multipart/form-data")) {
            return null;
        }
        int idx = contentType.indexOf("boundary=");
        if (idx < 0) {
            return null;
        }
        String boundary = contentType.substring(idx + "boundary=".length()).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() >= 2) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        int semi = boundary.indexOf(';');
        if (semi >= 0) {
            boundary = boundary.substring(0, semi).trim();
        }
        return boundary.isEmpty() ? null : boundary;
    }

    private static int indexOf(byte[] source, int fromInclusive, int toExclusive, byte[] target, int searchFrom) {
        int start = Math.max(fromInclusive, searchFrom);
        outer:
        for (int i = start; i <= toExclusive - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int skipLineBreak(byte[] body, int pos, int end) {
        if (pos < end && body[pos] == '\r') {
            pos++;
        }
        if (pos < end && body[pos] == '\n') {
            pos++;
        }
        return pos;
    }
}
