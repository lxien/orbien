package io.github.lxien.orbien.test.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 模拟内网 UDP 服务
 */
public final class LocalUdpBackendServer {
    private static final int LISTEN_PORT = 9998;
    private static final String BIND_HOST = "127.0.0.1";
    private static final int BUFFER_SIZE = 2048;
    private static final String REPLY_PREFIX = "backend-echo@9999: ";

    private LocalUdpBackendServer() {
    }

    public static void main(String[] args) throws Exception {
        InetSocketAddress bindAddress = new InetSocketAddress(BIND_HOST, LISTEN_PORT);
        System.out.printf("[LocalUdpBackend] 启动，监听 %s:%d%n", BIND_HOST, LISTEN_PORT);
        System.out.println("[LocalUdpBackend] 等待 orbien 客户端转发的 UDP 报文…（Ctrl+C 停止）");

        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(bindAddress);
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                socket.receive(incoming);

                String payload = new String(incoming.getData(), incoming.getOffset(), incoming.getLength(),
                        StandardCharsets.UTF_8);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                System.out.printf("[%s] 收到 %s:%d -> %s%n",
                        timestamp,
                        incoming.getAddress().getHostAddress(),
                        incoming.getPort(),
                        payload);

                String replyBody = REPLY_PREFIX + payload;
                byte[] replyBytes = replyBody.getBytes(StandardCharsets.UTF_8);
                DatagramPacket outgoing = new DatagramPacket(
                        replyBytes,
                        replyBytes.length,
                        incoming.getAddress(),
                        incoming.getPort());
                socket.send(outgoing);
                System.out.printf("[%s] 已回显 -> %s:%d%n",
                        timestamp,
                        incoming.getAddress().getHostAddress(),
                        incoming.getPort());
            }
        }
    }
}
