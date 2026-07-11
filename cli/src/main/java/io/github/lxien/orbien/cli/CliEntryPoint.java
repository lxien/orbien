package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.cli.support.CliBootstrap;
import picocli.CommandLine;

public final class CliEntryPoint {

    private CliEntryPoint() {
    }

    public static int execute(String[] args) {
        if (!CliBootstrap.isLegacyDebugMode(args)) {
            CliBootstrap.prepareQuietSession();
        }

        CommandLine commandLine = new CommandLine(new OrbienCommand());
        commandLine.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
            Throwable error = ex instanceof picocli.CommandLine.ExecutionException
                    && ex.getCause() != null ? ex.getCause() : ex;
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                System.err.println(message);
            } else {
                System.err.println("错误: " + error);
            }
            return 1;
        });
        return commandLine.execute(args);
    }
}
