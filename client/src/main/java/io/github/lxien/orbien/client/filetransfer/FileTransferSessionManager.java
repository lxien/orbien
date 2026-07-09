package io.github.lxien.orbien.client.filetransfer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferSessionManager {

    private final Map<String, UploadSession> uploads = new ConcurrentHashMap<>();

    public void startUpload(String requestId, String proxyId, String path) {
        uploads.put(requestId, new UploadSession(requestId, proxyId, path));
    }

    public UploadSession upload(String requestId) {
        return uploads.get(requestId);
    }

    public void finishUpload(String requestId) {
        uploads.remove(requestId);
    }

    public static class UploadSession {
        private final String requestId;
        private final String proxyId;
        private final String path;

        UploadSession(String requestId, String proxyId, String path) {
            this.requestId = requestId;
            this.proxyId = proxyId;
            this.path = path;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getProxyId() {
            return proxyId;
        }

        public String getPath() {
            return path;
        }
    }
}
