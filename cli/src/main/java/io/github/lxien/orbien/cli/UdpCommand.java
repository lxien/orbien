package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.cli.config.CliAppConfigFactory;
import io.github.lxien.orbien.cli.launcher.TunnelClientLauncher;
import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.cli.credentials.Credentials;
import io.github.lxien.orbien.cli.credentials.CredentialsStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "udp",
        description = "启动 UDP 代理"
)
public class UdpCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "本地端口")
    int port;

    @Option(names = "--host", defaultValue = "127.0.0.1", description = "本地服务主机地址")
    String host;

    @Option(names = "--remote-port", description = "远程端口；未指定时由服务端端口池分配")
    Integer remotePort;

    @Override
    public Integer call() {
        Credentials credentials = CredentialsStore.loadOrThrow();
        CredentialsStore.validatePort(port);
        if (remotePort != null) {
            CredentialsStore.validatePort(remotePort);
        }
        AppConfig config = CliAppConfigFactory.buildUdp(credentials, host, port, remotePort);
        return TunnelClientLauncher.launchSession(config);
    }
}
