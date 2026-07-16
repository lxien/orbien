package io.github.lxien.orbien.client;

import io.github.lxien.orbien.cli.CliEntryPoint;

public class OrbienClientStartup {

    public static void main(String[] args) {
        System.exit(CliEntryPoint.execute(args));
    }
}
