package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.cli.support.ServerAddressParser;
import io.github.lxien.orbien.credentials.Credentials;
import io.github.lxien.orbien.credentials.CredentialsStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

@Command(
        name = "login",
        description = "保存服务端凭据"
)
public class LoginCommand implements Callable<Integer> {

    @Option(names = "--server", required = true, description = "服务端地址，格式 host[:port]，默认端口 9527")
    String server;

    @Option(names = "--token", required = true, description = "访问令牌")
    String token;

    @Override
    public Integer call() {
        ServerAddressParser.ServerEndpoint endpoint =
                ServerAddressParser.parse(server, CredentialsStore.DEFAULT_SERVER_PORT);

        probeServer(endpoint);

        Credentials credentials = new Credentials();
        credentials.setServerAddr(endpoint.host());
        credentials.setServerPort(endpoint.port());
        credentials.setToken(token.trim());
        CredentialsStore.save(credentials);

        System.out.printf("凭据已保存至 %s%n", CredentialsStore.credentialsPath());
        return 0;
    }

    private void probeServer(ServerAddressParser.ServerEndpoint endpoint) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), 3000);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "无法连接服务端 " + endpoint.host() + ":" + endpoint.port(), e);
        }
    }
}
