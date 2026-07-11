package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.cli.launcher.TunnelClientLauncher;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "orbien",
        mixinStandardHelpOptions = true,
        version = "orbien 0.20.0",
        description = "Orbien 内网穿透客户端",
        subcommands = {
                LoginCommand.class,
                LogoutCommand.class,
                RunCommand.class,
                HttpCommand.class,
                TcpCommand.class,
                UdpCommand.class
        }
)
public class OrbienCommand implements Callable<Integer> {

    @Option(
            names = "-c",
            description = "配置文件路径（文件日志模式）"
    )
    String configFile;

    @Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        if (configFile != null) {
            return TunnelClientLauncher.launchLegacy(configFile);
        }
        spec.commandLine().usage(System.out);
        return 0;
    }
}
