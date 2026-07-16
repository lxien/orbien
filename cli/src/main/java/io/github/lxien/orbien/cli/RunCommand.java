package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.cli.config.RunAppConfigFactory;
import io.github.lxien.orbien.cli.launcher.TunnelClientLauncher;
import io.github.lxien.orbien.client.config.AppConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "run",
        description = "根据配置文件启动客户端"
)
public class RunCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "CONFIG",
            description = "配置文件路径；默认查找 ./orbien.toml、./config/orbien.toml"
    )
    String configFile;

    @Override
    public Integer call() {
        AppConfig config = RunAppConfigFactory.build(configFile);
        return TunnelClientLauncher.launchSession(config);
    }
}
