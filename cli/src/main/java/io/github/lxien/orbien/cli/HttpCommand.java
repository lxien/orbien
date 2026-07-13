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
        name = "http",
        description = "启动 HTTP 代理"
)
public class HttpCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "本地端口")
    int port;

    @Option(names = "--host", defaultValue = "127.0.0.1", description = "本地服务主机地址")
    String host;

    @Option(names = "--domain", description = "子域名前缀")
    String domain;

    @Override
    public Integer call() {
        Credentials credentials = CredentialsStore.loadOrThrow();
        CredentialsStore.validatePort(port);
        AppConfig config = CliAppConfigFactory.buildHttp(credentials, host, port, domain);
        return TunnelClientLauncher.launchSession(config);
    }
}
