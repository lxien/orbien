package io.github.lxien.orbien.core.filetransfer;

public final class FileTransferConstants {

    public static final int CHUNK_SIZE = 256 * 1024;
    public static final int SESSION_TTL_MINUTES = 60;
    /**
     * 列表、重命名等短请求超时
     */
    public static final long REQUEST_TIMEOUT_MS = 30_000L;
    /**
     * 删除操作最短超时（含非空目录递归删除）
     */
    public static final long DELETE_MIN_TIMEOUT_MS = 120_000L;
    /**
     * 删除操作最长超时
     */
    public static final long DELETE_MAX_TIMEOUT_MS = 30 * 60_000L;
    /**
     * 移动操作最短超时（含非空目录递归移动）
     */
    public static final long MOVE_MIN_TIMEOUT_MS = 120_000L;
    /**
     * 移动操作最长超时
     */
    public static final long MOVE_MAX_TIMEOUT_MS = 30 * 60_000L;
    /**
     * 移动操作每 MB 额外超时
     */
    public static final long MOVE_TIMEOUT_PER_MB_MS = 2_000L;
    /**
     * 文件传输最短超时
     */
    public static final long TRANSFER_MIN_TIMEOUT_MS = 60_000L;
    /**
     * 文件传输最长超时（覆盖 500MB 上限场景）
     */
    public static final long TRANSFER_MAX_TIMEOUT_MS = 30 * 60_000L;
    /**
     * 每 MB 额外增加的传输超时
     */
    public static final long TRANSFER_TIMEOUT_PER_MB_MS = 3_000L;
    /**
     * 单个分块发送超时
     */
    public static final long CHUNK_SEND_TIMEOUT_MS = 15_000L;

    public static long transferTimeoutMs(long totalBytes) {
        if (totalBytes <= 0) {
            return TRANSFER_MIN_TIMEOUT_MS;
        }
        long mb = (totalBytes + 1024 * 1024 - 1) / (1024 * 1024);
        long timeout = TRANSFER_MIN_TIMEOUT_MS + mb * TRANSFER_TIMEOUT_PER_MB_MS;
        return Math.min(TRANSFER_MAX_TIMEOUT_MS, timeout);
    }

    public static final String PERMISSION_READ = "read";
    public static final String PERMISSION_READ_WRITE = "read_write";

    public static final String OP_MKDIR = "mkdir";
    public static final String OP_DELETE = "delete";
    public static final String OP_MOVE = "move";
    public static final String OP_RENAME = "rename";

    public static long moveTimeoutMs(long totalBytes) {
        if (totalBytes <= 0) {
            return MOVE_MIN_TIMEOUT_MS;
        }
        long mb = (totalBytes + 1024 * 1024 - 1) / (1024 * 1024);
        long timeout = MOVE_MIN_TIMEOUT_MS + mb * MOVE_TIMEOUT_PER_MB_MS;
        return Math.min(MOVE_MAX_TIMEOUT_MS, timeout);
    }

    private FileTransferConstants() {
    }
}
