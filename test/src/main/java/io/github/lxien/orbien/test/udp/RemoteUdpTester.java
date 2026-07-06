package io.github.lxien.orbien.test.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 模拟远程访客，向 orbien 服务端 UDP 代理端口发送测试报文并等待回包。
 */
public final class RemoteUdpTester {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_UDP_PORT = 9090;
    private static final int LOCAL_BIND_PORT = 0;
    private static final int RECEIVE_TIMEOUT_MS = 5000;
    private static final int BUFFER_SIZE = 2048;
    private static final List<String> TEST_MESSAGES = List.of(
            "hello-udp-from-remote-tester",
            "ping-orbien-udp-tunnel",
            "message-with-中文-payload"
    );
    private static final long SEND_INTERVAL_MS = 800L;

    private RemoteUdpTester() {
    }

    public static void main(String[] args) throws Exception {
        InetSocketAddress serverAddress = new InetSocketAddress(SERVER_HOST, SERVER_UDP_PORT);
        System.out.printf("[RemoteUdpTester] 目标服务端 UDP 代理 %s:%d%n", SERVER_HOST, SERVER_UDP_PORT);
        System.out.printf("[RemoteUdpTester] 将发送 %d 条测试报文%n", TEST_MESSAGES.size());

        try (DatagramSocket socket = new DatagramSocket(LOCAL_BIND_PORT)) {
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
            InetSocketAddress local = (InetSocketAddress) socket.getLocalSocketAddress();
            System.out.printf("[RemoteUdpTester] 本地 UDP 端口 %d%n", local.getPort());

            for (int i = 0; i < TEST_MESSAGES.size(); i++) {
                String message = TEST_MESSAGES.get(i);
                sendAndReceive(socket, serverAddress, message, i + 1);
            }
        }

        System.out.println("[RemoteUdpTester] 测试完成");
    }

    private static void sendAndReceive(DatagramSocket socket,
                                       InetSocketAddress serverAddress,
                                       String message,
                                       int sequence) throws Exception {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket outgoing = new DatagramPacket(payload, payload.length, serverAddress);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        socket.send(outgoing);
        System.out.printf("[%s] #%d 已发送 -> %s:%d : %s%n",
                timestamp,
                sequence,
                serverAddress.getHostString(),
                serverAddress.getPort(),
                message);

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(incoming);
            String reply = new String(incoming.getData(), incoming.getOffset(), incoming.getLength(),
                    StandardCharsets.UTF_8);
            System.out.printf("[%s] #%d 收到回包 <- %s:%d : %s%n",
                    timestamp,
                    sequence,
                    incoming.getAddress().getHostAddress(),
                    incoming.getPort(),
                    reply);
        } catch (java.net.SocketTimeoutException e) {
            System.out.printf("[%s] #%d 等待回包超时（%d ms），请确认：%n",
                    timestamp, sequence, RECEIVE_TIMEOUT_MS);
            System.out.println("  1. orbien 服务端已启动且 UDP 代理已启用");
            System.out.println("  2. 客户端在线且 local_port=9999 的后端已运行 LocalUdpBackendServer");
            System.out.printf("  3. Console 中 UDP 代理 remote_port 为 %d%n", SERVER_UDP_PORT);
        }
    }
}
